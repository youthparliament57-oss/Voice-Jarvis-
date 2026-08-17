import re

with open("gradle/libs.versions.toml", "r") as f:
    content = f.read()

content = content.replace('agp = "8.7.3"', 'agp = "9.1.1"')
content = content.replace('kotlin = "2.0.21"', 'kotlin = "2.2.10"')

with open("gradle/libs.versions.toml", "w") as f:
    f.write(content)
