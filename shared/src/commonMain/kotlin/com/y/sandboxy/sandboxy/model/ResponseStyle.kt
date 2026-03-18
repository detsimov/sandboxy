package com.y.sandboxy.sandboxy.model

enum class ResponseStyle(
    val displayName: String,
    val suffix: String,
) {
    Default("Default", ""),
    CodeOnly(
        "Code only",
        "Respond ONLY with code inside a single fenced code block (```). No explanations, no comments outside the block, no preamble."
    ),
    Detailed(
        "Detailed",
        "Provide thorough, comprehensive responses. Explain your reasoning and cover edge cases."
    ),
    Council(
        "Council",
        """You are a council of four experts debating a problem. For every task, each expert must independently propose their solution with reasoning:\n" +
                "\n" +
                "1. **Mathematician** — approaches the problem through logic, formulas, and precise reasoning.\n" +
                "2. **Engineer** — focuses on practical, efficient, and implementable solutions.\n" +
                "3. **Analyst** — examines data, patterns, edge cases, and risks.\n" +
                "4. **Critic** — challenges every solution, finds flaws, and stress-tests assumptions.\n" +
                "\n" +
                "After all four have spoken, hold a **Round Table**: experts respond to each other's points, refine ideas, and resolve disagreements. Then produce a **Final Verdict** — the strongest combined solution that survived the critique."""
    ),
    StepByStep(
        "Step-by-step",
        "Solve every problem step by step: first, clearly restate what is being asked, then break it into smaller parts and work through each one showing your reasoning. After reaching a conclusion, verify your answer before presenting the final result."
    ),
    ProsCons(
        "Pros & Cons",
        "Analyze the topic by listing pros and cons in two separate groups, then give a brief verdict."
    ),
    Markdown(
        "Markdown",
        "Format your response using markdown: use headers, bullet lists, bold/italic for emphasis, and code blocks where appropriate."
    ),
    Structured(
        "Structured",
        "Organize your response with clear numbered sections, each with a descriptive heading. Use a logical structure."
    ),
}
