import pg from "pg";
import dotenv from "dotenv";

dotenv.config();

import path from "path";

// Attempt to load .env from current directory or root directory
dotenv.config();
if (!process.env.DATABASE_URL) {
  dotenv.config({ path: path.resolve(process.cwd(), "../.env") });
}

const DATABASE_URL = process.env.DATABASE_URL;

if (!DATABASE_URL) {
  console.warn("⚠️ WARNING: DATABASE_URL is not set in environment variables! Database operations may fail.");
}

export const pool = new pg.Pool({
  connectionString: DATABASE_URL || "postgresql://postgres:postgres@localhost:5432/learning_platform",
  ssl: DATABASE_URL && DATABASE_URL.includes("neon.tech") ? { rejectUnauthorized: false } : undefined
});

export async function getPlatformSummary() {
  const statsQuery = await pool.query(`
    SELECT 
      (SELECT COUNT(*) FROM users) AS total_users,
      (SELECT COUNT(*) FROM users WHERE role = 'STUDENT') AS total_students,
      (SELECT COUNT(*) FROM users WHERE role = 'ADMIN') AS total_admins,
      (SELECT COUNT(*) FROM courses) AS total_courses,
      (SELECT COUNT(*) FROM lessons) AS total_lessons,
      (SELECT COUNT(*) FROM enrollments) AS total_enrollments;
  `);
  return statsQuery.rows[0];
}

export async function listAvailableCourses() {
  const coursesRes = await pool.query(`
    SELECT 
      c.id, 
      c.title, 
      c.description, 
      c.price, 
      c.category, 
      c.status,
      (SELECT COUNT(*) FROM chapters ch WHERE ch.course_id = c.id) AS total_chapters,
      (SELECT COUNT(*) FROM lessons l WHERE l.course_id = c.id) AS total_lessons
    FROM courses c
    WHERE c.status = 'PUBLISHED' OR c.status IS NULL
    ORDER BY c.title ASC;
  `);
  return coursesRes.rows;
}

export async function getStudentProfileAndProgress(email: string) {
  const userRes = await pool.query(
    "SELECT id, email, role FROM users WHERE email = $1",
    [email.trim()]
  );

  if (userRes.rows.length === 0) {
    return { error: `No user found with email: ${email}` };
  }

  const student = userRes.rows[0];

  const enrollmentsRes = await pool.query(
    `
    SELECT 
      c.id AS course_id,
      c.title AS course_title,
      c.category,
      e.enrolled_at,
      (SELECT COUNT(*) FROM lessons l WHERE l.course_id = c.id) AS total_lessons,
      (SELECT COUNT(*) FROM course_progress cp WHERE cp.student_id = $1 AND cp.course_id = c.id AND cp.is_completed = true) AS completed_lessons
    FROM enrollments e
    JOIN courses c ON e.course_id = c.id
    WHERE e.student_id = $1
    ORDER BY e.enrolled_at DESC;
    `,
    [student.id]
  );

  return {
    student_id: student.id,
    email: student.email,
    role: student.role,
    total_enrolled_courses: enrollmentsRes.rows.length,
    enrollments: enrollmentsRes.rows.map((row) => {
      const total = Number(row.total_lessons) || 0;
      const completed = Number(row.completed_lessons) || 0;
      const progressPercentage = total > 0 ? Math.round((completed / total) * 100) : 0;
      return {
        course_id: row.course_id,
        title: row.course_title,
        category: row.category,
        enrolled_at: row.enrolled_at,
        total_lessons: total,
        completed_lessons: completed,
        progress: `${progressPercentage}%`
      };
    })
  };
}

export async function searchCourseCurriculum(keyword: string) {
  const searchPattern = `%${keyword.trim()}%`;
  const res = await pool.query(
    `
    SELECT 
      c.title AS course_title,
      ch.title AS chapter_title,
      l.title AS lesson_title,
      l.lesson_type,
      l.sequence_order
    FROM lessons l
    LEFT JOIN chapters ch ON l.chapter_id = ch.id
    LEFT JOIN courses c ON l.course_id = c.id
    WHERE l.title ILIKE $1 
       OR l.description ILIKE $1 
       OR ch.title ILIKE $1 
       OR c.title ILIKE $1
    ORDER BY c.title, ch.sequence_order, l.sequence_order
    LIMIT 20;
    `,
    [searchPattern]
  );
  return res.rows;
}

export async function getCourseStructure(courseQuery: string) {
  const courseRes = await pool.query(
    `
    SELECT id, title, description, price, category, status 
    FROM courses 
    WHERE id::text = $1 OR title ILIKE $2
    LIMIT 1;
    `,
    [courseQuery.trim(), `%${courseQuery.trim()}%`]
  );

  if (courseRes.rows.length === 0) {
    return { error: `Course not found for query: "${courseQuery}"` };
  }

  const course = courseRes.rows[0];

  const structureRes = await pool.query(
    `
    SELECT 
      ch.id AS chapter_id,
      ch.title AS chapter_title,
      ch.sequence_order AS chapter_order,
      l.id AS lesson_id,
      l.title AS lesson_title,
      l.sequence_order AS lesson_order,
      l.lesson_type
    FROM chapters ch
    LEFT JOIN lessons l ON ch.id = l.chapter_id
    WHERE ch.course_id = $1
    ORDER BY ch.sequence_order, l.sequence_order;
    `,
    [course.id]
  );

  return {
    course: course,
    total_entries: structureRes.rows.length,
    curriculum: structureRes.rows
  };
}

export async function adminEnrollStudent(studentEmail: string, courseKeyword: string) {
  const userRes = await pool.query(
    "SELECT id, email FROM users WHERE email = $1",
    [studentEmail.trim()]
  );

  if (userRes.rows.length === 0) {
    return { error: `Student with email ${studentEmail} does not exist.` };
  }

  const studentId = userRes.rows[0].id;

  const courseRes = await pool.query(
    "SELECT id, title FROM courses WHERE title ILIKE $1 LIMIT 1",
    [`%${courseKeyword.trim()}%`]
  );

  if (courseRes.rows.length === 0) {
    return { error: `No course found matching "${courseKeyword}".` };
  }

  const courseId = courseRes.rows[0].id;
  const courseTitle = courseRes.rows[0].title;

  const existing = await pool.query(
    "SELECT id FROM enrollments WHERE student_id = $1 AND course_id = $2",
    [studentId, courseId]
  );

  if (existing.rows.length > 0) {
    return { error: `Student ${studentEmail} is ALREADY enrolled in "${courseTitle}".` };
  }

  const insertRes = await pool.query(
    "INSERT INTO enrollments (id, student_id, course_id, enrolled_at) VALUES (gen_random_uuid(), $1, $2, NOW()) RETURNING id, enrolled_at",
    [studentId, courseId]
  );

  return {
    success: true,
    message: `Student successfully enrolled!`,
    enrollment_id: insertRes.rows[0].id,
    student_email: studentEmail,
    course_title: courseTitle,
    enrolled_at: insertRes.rows[0].enrolled_at
  };
}
