import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import {
  getPlatformSummary,
  listAvailableCourses,
  getStudentProfileAndProgress,
  searchCourseCurriculum,
  getCourseStructure,
  adminEnrollStudent
} from "./tools.js";

const server = new McpServer({
  name: "learning-platform-agent",
  version: "1.0.0"
});

// Tool 0: List Available Courses (Public for Guests/Students)
server.tool(
  "list_available_courses",
  {},
  async () => {
    const data = await listAvailableCourses();
    return {
      content: [{ type: "text", text: JSON.stringify(data, null, 2) }]
    };
  }
);

// Tool 1: Overall Platform Summary
server.tool(
  "get_platform_summary",
  {},
  async () => {
    const data = await getPlatformSummary();
    return {
      content: [{ type: "text", text: JSON.stringify(data, null, 2) }]
    };
  }
);

// Tool 2: Student Profile, Enrolled Courses & Progress
server.tool(
  "get_student_profile_and_progress",
  {
    email: z.string().describe("The email address of the student")
  },
  async ({ email }) => {
    const data = await getStudentProfileAndProgress(email);
    return {
      content: [{ type: "text", text: JSON.stringify(data, null, 2) }]
    };
  }
);

// Tool 3: Search Course Curriculum (Lessons & Chapters)
server.tool(
  "search_course_curriculum",
  {
    keyword: z.string().describe("Topic or keyword to search (e.g. 'Security', 'REST', 'Docker')")
  },
  async ({ keyword }) => {
    const data = await searchCourseCurriculum(keyword);
    return {
      content: [{ type: "text", text: JSON.stringify(data, null, 2) }]
    };
  }
);

// Tool 4: Get Course Structure & Lesson Breakdown
server.tool(
  "get_course_structure",
  {
    course_query: z.string().describe("Course title keyword or course UUID")
  },
  async ({ course_query }) => {
    const data = await getCourseStructure(course_query);
    return {
      content: [{ type: "text", text: JSON.stringify(data, null, 2) }]
    };
  }
);

// Tool 5: Safe Admin Enrollment
server.tool(
  "admin_enroll_student",
  {
    student_email: z.string().describe("Email of the student to enroll"),
    course_keyword: z.string().describe("Course title keyword to match course")
  },
  async ({ student_email, course_keyword }) => {
    const data = await adminEnrollStudent(student_email, course_keyword);
    return {
      content: [{ type: "text", text: JSON.stringify(data, null, 2) }]
    };
  }
);

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

main().catch((error) => {
  console.error("Fatal error running Learning Platform MCP Server:", error);
  process.exit(1);
});
