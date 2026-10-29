import express, { Request, Response } from "express";
import cors from "cors";
import dotenv from "dotenv";
import {
  getPlatformSummary,
  listAvailableCourses,
  getStudentProfileAndProgress,
  searchCourseCurriculum,
  getCourseStructure,
  adminEnrollStudent
} from "./tools.js";

dotenv.config();

const app = express();
const PORT = process.env.PORT || 8089;

app.use(cors());
app.use(express.json());

// Health Check Endpoint
app.get("/health", (_req: Request, res: Response) => {
  res.json({ status: "UP", service: "learning-platform-ai-service", port: PORT });
});

app.get("/api/v1/ai/health", (_req: Request, res: Response) => {
  res.json({ status: "UP", service: "learning-platform-ai-service", port: PORT });
});

interface ChatRequestBody {
  message: string;
}

// Main AI Chat Handler with RBAC Enforcement & Public Guest Access
async function handleChat(req: Request<{}, {}, ChatRequestBody>, res: Response) {
  try {
    const { message } = req.body;
    if (!message || typeof message !== "string") {
      return res.status(400).json({ error: "Missing 'message' string in request body." });
    }

    // Headers injected by Spring Cloud Gateway (JwtAuthenticationFilter) or Client
    const userEmail = (req.headers["x-user-email"] as string) || "";
    const userRoleRaw = (req.headers["x-user-role"] as string) || "";

    const isAdmin = userRoleRaw.toUpperCase().includes("ADMIN");
    const isStudent = userRoleRaw.toUpperCase().includes("STUDENT");
    const isAuthenticated = Boolean(userEmail);

    const query = message.trim();
    const queryLower = query.toLowerCase();

    // -------------------------------------------------------------
    // INTENT 1: Admin Actions (Direct Enrollment)
    // -------------------------------------------------------------
    if (queryLower.includes("enroll") && (queryLower.includes("@") || (isAdmin && queryLower.includes("student")))) {
      if (!isAdmin) {
        return res.json({
          reply: `🚫 **Access Denied**: Direct course enrollment is reserved for administrators only. As a student or guest, please click the **Enroll Now** button on the course page or register for an account.`,
          user: { email: userEmail, role: userRoleRaw || "GUEST" },
          toolsUsed: []
        });
      }

      // Extract email & course title from query
      const emailMatch = query.match(/[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/);
      const studentToEnroll = emailMatch ? emailMatch[0] : "";
      
      let courseKeyword = "Spring Boot";
      if (queryLower.includes("dsa")) courseKeyword = "DSA";
      else if (queryLower.includes("spring")) courseKeyword = "Spring Boot";

      if (!studentToEnroll) {
        return res.json({
          reply: `Please provide the student's email address to enroll. (e.g. *"Enroll student@example.com in Spring Boot"*).`,
          user: { email: userEmail, role: userRoleRaw }
        });
      }

      const result = await adminEnrollStudent(studentToEnroll, courseKeyword);
      if ("error" in result) {
        return res.json({
          reply: `⚠️ Enrollment failed: ${result.error}`,
          user: { email: userEmail, role: userRoleRaw },
          toolsUsed: ["admin_enroll_student"]
        });
      }

      return res.json({
        reply: `✅ **Enrollment Successful!**\n\nStudent **${result.student_email}** has been enrolled in **"${result.course_title}"**.\n- Enrollment ID: \`${result.enrollment_id}\`\n- Time: ${new Date(result.enrolled_at).toLocaleString()}`,
        user: { email: userEmail, role: userRoleRaw },
        toolsUsed: ["admin_enroll_student"]
      });
    }

    // -------------------------------------------------------------
    // INTENT 2: Platform Overview / Admin Summary
    // -------------------------------------------------------------
    if (queryLower.includes("summary") || queryLower.includes("stats") || queryLower.includes("total users") || queryLower.includes("overview")) {
      if (!isAdmin) {
        return res.json({
          reply: `🔒 **Restricted Info**: Platform metrics and student analytics are visible to Administrators only. To explore public courses, ask *"What courses are available?"*`,
          user: { email: userEmail, role: userRoleRaw || "GUEST" },
          toolsUsed: []
        });
      }

      const summary = await getPlatformSummary();
      return res.json({
        reply: `📊 **Platform Operations Summary**\n\n- 👥 **Total Users:** ${summary.total_users} (${summary.total_students} Students, ${summary.total_admins} Admins)\n- 📚 **Total Courses:** ${summary.total_courses}\n- 🎥 **Total Lessons:** ${summary.total_lessons}\n- 🎓 **Active Enrollments:** ${summary.total_enrollments}`,
        user: { email: userEmail, role: userRoleRaw },
        toolsUsed: ["get_platform_summary"]
      });
    }

    // -------------------------------------------------------------
    // INTENT 3: Public Course Catalog / Available Courses (Open to All / Guests)
    // -------------------------------------------------------------
    if (
      queryLower.includes("available") ||
      queryLower.includes("catalog") ||
      queryLower.includes("all course") ||
      queryLower.includes("courses list") ||
      queryLower.includes("what courses") ||
      queryLower.includes("which courses") ||
      queryLower.includes("kya courses") ||
      queryLower.includes("list course") ||
      queryLower.includes("show courses") ||
      queryLower.includes("browse")
    ) {
      const courses = await listAvailableCourses();
      if (!courses || courses.length === 0) {
        return res.json({
          reply: `Currently there are no published courses available in the catalog. Please check back soon!`,
          user: { email: userEmail, role: userRoleRaw || "GUEST" },
          toolsUsed: ["list_available_courses"]
        });
      }

      const coursesFormatted = courses
        .map(
          (c: any, idx: number) =>
            `### ${idx + 1}. 📘 ${c.title}\n- 🏷️ **Category:** ${c.category}\n- 💰 **Price:** $${c.price || "Free"}\n- 📖 **Modules:** ${c.total_chapters || 0} Chapters • ${c.total_lessons || 0} Lessons\n- 📝 **Summary:** ${c.description || "Industry-ready curriculum designed for hands-on mastery."}`
        )
        .join("\n\n");

      return res.json({
        reply: `🎓 **Available Courses on Gaurav's CDN Learning Platform**\n\n${coursesFormatted}\n\n💡 *Tip: Ask me "Show syllabus for Spring Boot" or "Tell me about DSA" to inspect full chapter breakdowns!*`,
        user: { email: userEmail, role: userRoleRaw || "GUEST" },
        toolsUsed: ["list_available_courses"]
      });
    }

    // -------------------------------------------------------------
    // INTENT 4: Pricing & How to Enroll Guide (Open to Guests)
    // -------------------------------------------------------------
    if (
      queryLower.includes("how to enroll") ||
      queryLower.includes("how can i enroll") ||
      queryLower.includes("how to buy") ||
      queryLower.includes("how to register") ||
      queryLower.includes("how to join") ||
      queryLower.includes("price") ||
      queryLower.includes("cost") ||
      queryLower.includes("fees") ||
      queryLower.includes("pricing") ||
      queryLower.includes("kaise enroll")
    ) {
      const courses = await listAvailableCourses();
      const priceSummary = courses
        .map((c: any) => `- **${c.title}**: **$${c.price || "12.99"}** (Lifetime Access)`)
        .join("\n");

      return res.json({
        reply: `💳 **Course Pricing & How to Enroll**\n\n${priceSummary}\n\n### 🚀 How to Enroll in 3 Simple Steps:\n1. **Create an Account:** Click the **Sign Up** / **Register** button in the top navigation.\n2. **Select Your Course:** Browse our catalog and click on any course card.\n3. **Enroll Instantly:** Click **Enroll Now** to get immediate access to all video lessons, project files, and downloadable notes!\n\nAll courses include a verifiable Certificate of Completion! 🏆`,
        user: { email: userEmail, role: userRoleRaw || "GUEST" },
        toolsUsed: ["list_available_courses"]
      });
    }

    // -------------------------------------------------------------
    // INTENT 5: Student Progress & Enrolled Courses
    // -------------------------------------------------------------
    if (queryLower.includes("progress") || queryLower.includes("completed") || queryLower.includes("enrolled") || queryLower.includes("my courses")) {
      if (!isAuthenticated) {
        return res.json({
          reply: `🎓 **Guest Visitor Mode**\n\nYou are currently browsing as a guest. To view your personal course progress, completed lessons, and certificates, please **Log In** or **Register** an account.\n\nIn the meantime, feel free to ask me:\n- *"What courses are available?"*\n- *"Show syllabus for Spring Boot"*\n- *"Search lessons on REST APIs"*`,
          user: { email: "", role: "GUEST" },
          toolsUsed: []
        });
      }

      // Check if querying someone else's email
      const emailMatch = query.match(/[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/);
      const targetEmail = emailMatch ? emailMatch[0] : "";

      let emailToLookup = userEmail;
      if (targetEmail && targetEmail.toLowerCase() !== userEmail.toLowerCase()) {
        if (!isAdmin) {
          return res.json({
            reply: `🔒 **Privacy Guard**: As a student, you can only view your own progress. You are not authorized to view progress for **${targetEmail}**.\n\nShowing your progress instead...`,
            user: { email: userEmail, role: userRoleRaw },
            toolsUsed: []
          });
        } else {
          emailToLookup = targetEmail;
        }
      }

      const report = await getStudentProfileAndProgress(emailToLookup);
      if ("error" in report) {
        return res.json({
          reply: `No student records found for: **${emailToLookup}**`,
          user: { email: userEmail, role: userRoleRaw },
          toolsUsed: ["get_student_profile_and_progress"]
        });
      }

      if (report.enrollments.length === 0) {
        return res.json({
          reply: `**Student:** ${report.email}\nYou are not enrolled in any courses yet. Browse our catalog to get started!`,
          user: { email: userEmail, role: userRoleRaw },
          toolsUsed: ["get_student_profile_and_progress"]
        });
      }

      const coursesFormatted = report.enrollments
        .map(
          (e) =>
            `### 📘 ${e.title}\n- **Category:** ${e.category}\n- **Completed:** ${e.completed_lessons} / ${e.total_lessons} lessons\n- **Progress:** **${e.progress}** ${e.progress === "100%" ? "✅ (Completed)" : "⏳"}`
        )
        .join("\n\n");

      return res.json({
        reply: `🎓 **Learning Progress Report for ${report.email}**\n\nTotal Enrolled Courses: **${report.total_enrolled_courses}**\n\n${coursesFormatted}`,
        user: { email: userEmail, role: userRoleRaw },
        toolsUsed: ["get_student_profile_and_progress"]
      });
    }

    // -------------------------------------------------------------
    // INTENT 6: Course Syllabus / Chapter Breakdown (Dynamic for Any Course)
    // -------------------------------------------------------------
    if (
      queryLower.includes("chapter") ||
      queryLower.includes("syllabus") ||
      queryLower.includes("breakdown") ||
      queryLower.includes("structure") ||
      queryLower.includes("curriculum") ||
      queryLower.includes("topics")
    ) {
      let targetCourse = "Spring";
      if (queryLower.includes("dsa")) targetCourse = "DSA";
      else if (queryLower.includes("spring")) targetCourse = "Spring";

      const data = await getCourseStructure(targetCourse);
      if ("error" in data) {
        return res.json({ reply: data.error, user: { email: userEmail, role: userRoleRaw || "GUEST" } });
      }

      const lessonsList = data.curriculum
        .map((c: any) => `* **${c.chapter_title}** → *${c.lesson_title}* [${c.lesson_type || "VIDEO"}]`)
        .slice(0, 15)
        .join("\n");

      return res.json({
        reply: `📚 **Course Curriculum: ${data.course.title}**\n\n*${data.course.description || ""}*\n\n**Category:** ${data.course.category} | **Price:** $${data.course.price}\n\n**Lessons Overview:**\n${lessonsList}\n\n*(Total ${data.total_entries} curriculum modules available)*`,
        user: { email: userEmail, role: userRoleRaw || "GUEST" },
        toolsUsed: ["get_course_structure"]
      });
    }

    // -------------------------------------------------------------
    // INTENT 7: Curriculum & Topic Search (Default / Open to all)
    // -------------------------------------------------------------
    let keyword = query
      .replace(/what|is|are|available|for|tell|me|about|can|you|search|find|kya|hai|batao|related|lessons|topics|kaha|lesson|show|courses|course/gi, "")
      .replace(/[?.,!]/g, "")
      .trim();
    if (!keyword) keyword = "Spring";

    const searchResults = await searchCourseCurriculum(keyword);

    if (searchResults.length > 0) {
      const resultsFormatted = searchResults
        .slice(0, 5)
        .map(
          (r: any, idx: number) =>
            `${idx + 1}. **${r.lesson_title}**\n   - Course: *${r.course_title}*\n   - Chapter: *${r.chapter_title}*`
        )
        .join("\n\n");

      return res.json({
        reply: `🔍 Found **${searchResults.length} lessons** related to **"${keyword}"**:\n\n${resultsFormatted}\n\nNeed help with any specific topic? Feel free to ask!`,
        user: { email: userEmail, role: userRoleRaw || "GUEST" },
        toolsUsed: ["search_course_curriculum"]
      });
    }

    return res.json({
      reply: `I couldn't find any specific lessons matching **"${keyword}"**. You can ask me about:\n- 🎓 *"What courses are available?"*\n- 📚 *"Show syllabus for Spring Boot"* or *"DSA"* \n- 💳 *"How to enroll?"*\n- 🔍 Any topic like *REST APIs, CRUD, Project Structure*`,
      user: { email: userEmail, role: userRoleRaw || "GUEST" },
      toolsUsed: ["search_course_curriculum"]
    });
  } catch (error: any) {
    console.error("AI Chat handler error:", error);
    res.status(500).json({ error: "Internal server error processing AI chat", details: error.message });
  }
}

// Routes
app.post("/api/v1/ai/chat", handleChat);
app.post("/chat", handleChat);

app.listen(PORT, () => {
  console.log(`🚀 Learning Platform AI Service running on port ${PORT}`);
  console.log(`👉 Health check: http://localhost:${PORT}/health`);
  console.log(`👉 Chat endpoint: http://localhost:${PORT}/api/v1/ai/chat`);
});
