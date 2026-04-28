#!/usr/bin/env python3
import sys
import os

def ask_gemini(question):
    from google import genai
    client = genai.Client(api_key=os.environ.get("GEMINI_API_KEY"))
    response = client.models.generate_content(
        model="gemini-2.0-flash",
        contents=question,
    )
    return response.text

def ask_claude(question):
    from anthropic import Anthropic
    client = Anthropic(api_key=os.environ.get("ANTHROPIC_API_KEY"))
    response = client.messages.create(
        model="claude-opus-4-6",
        max_tokens=1000,
        messages=[{"role": "user", "content": question}]
    )
    return response.content[0].text

def ask_groq(question):
    from groq import Groq
    client = Groq(api_key=os.environ.get("GROQ_API_KEY"))
    response = client.chat.completions.create(
        model="llama-3.3-70b-versatile",
        messages=[{"role": "user", "content": question}]
    )
    return response.choices[0].message.content

full_command = " ".join(sys.argv[1:])

if full_command.startswith("(gemini)"):
    question = full_command[8:].strip()
    print(ask_gemini(question))
elif full_command.startswith("(claude)"):
    question = full_command[8:].strip()
    print(ask_claude(question))
elif full_command.startswith("(groq)"):
    question = full_command[6:].strip()
    print(ask_groq(question))
else:
    print(ask_gemini(full_command))