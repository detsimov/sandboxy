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
    StepByStep(
        "Step-by-step",
        "Break your response into clear numbered steps. Each step should be one concrete action or idea."
    ),
    TLDR(
        "TL;DR first",
        "Start with a 1-2 sentence TL;DR summary, then provide the full explanation below it."
    ),
    ProsCons(
        "Pros & Cons",
        "Analyze the topic by listing pros and cons in two separate groups, then give a brief verdict."
    ),
    TableFormat(
        "Table",
        "Where possible, organize information into a table or comparison grid for easy scanning."
    ),
    FAQ(
        "FAQ",
        "Structure your response as a list of frequently asked questions with concise answers."
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
