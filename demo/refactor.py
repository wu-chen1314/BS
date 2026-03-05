import os
import re

base_dir = r"d:\BS\ich-frontend\demo\src\main\java\com\example\demo"

def ensure_dir(path):
    if not os.path.exists(path):
        os.makedirs(path)

# 1. Create new directories
ensure_dir(os.path.join(base_dir, "common", "exception"))
ensure_dir(os.path.join(base_dir, "common", "result"))
ensure_dir(os.path.join(base_dir, "common", "constants"))
ensure_dir(os.path.join(base_dir, "model", "dto"))
ensure_dir(os.path.join(base_dir, "model", "vo"))
ensure_dir(os.path.join(base_dir, "aspect"))

# 2. Re-arrange files
def move_file(src_subpath, dest_subpath):
    src = os.path.join(base_dir, src_subpath)
    dst = os.path.join(base_dir, dest_subpath)
    if os.path.exists(src):
        os.rename(src, dst)

# AOP
if os.path.exists(os.path.join(base_dir, "aop", "LogAspect.java")):
    move_file(r"aop\LogAspect.java", r"aspect\LogAspect.java")
    try: os.rmdir(os.path.join(base_dir, "aop"))
    except: pass

# Result
move_file(r"common\Result.java", r"common\result\Result.java")

# VO
move_file(r"service\CategoryStatisticsVO.java", r"model\vo\CategoryStatisticsVO.java")
move_file(r"service\RegionStatisticsVO.java", r"model\vo\RegionStatisticsVO.java")
move_file(r"entity\CategoryTreeVO.java", r"model\vo\CategoryTreeVO.java")
move_file(r"entity\CommentVO.java", r"model\vo\CommentVO.java")
move_file(r"entity\RegionCategoryVO.java", r"model\vo\RegionCategoryVO.java")

# DTO
req_dir = os.path.join(base_dir, "request")
if os.path.exists(req_dir):
    for f in os.listdir(req_dir):
        move_file(os.path.join("request", f), os.path.join("model", "dto", f))
    try: os.rmdir(req_dir)
    except: pass

# 3. Replace text in all .java files
patterns = [
    (r"package com\.example\.demo\.aop;", r"package com.example.demo.aspect;"),
    (r"import com\.example\.demo\.aop\.", r"import com.example.demo.aspect."),
    (r"package com\.example\.demo\.common;", r"package com.example.demo.common.result;"),
    (r"import com\.example\.demo\.common\.Result;", r"import com.example.demo.common.result.Result;"),
    (r"package com\.example\.demo\.request;", r"package com.example.demo.model.dto;"),
    (r"import com\.example\.demo\.request\.", r"import com.example.demo.model.dto."),
    (r"package com\.example\.demo\.entity;\s*(\n.*)*\s*public class (CategoryTreeVO|CommentVO|RegionCategoryVO)", r"package com.example.demo.model.vo;\1public class \2"),
    (r"import com\.example\.demo\.entity\.(CategoryTreeVO|CommentVO|RegionCategoryVO);", r"import com.example.demo.model.vo.\1;"),
    (r"package com\.example\.demo\.service;\s*(\n.*)*\s*public class (CategoryStatisticsVO|RegionStatisticsVO)", r"package com.example.demo.model.vo;\1public class \2"),
    (r"import com\.example\.demo\.service\.(CategoryStatisticsVO|RegionStatisticsVO);", r"import com.example.demo.model.vo.\1;")
]

import fnmatch
for root, dirs, files in os.walk(base_dir):
    for item in fnmatch.filter(files, "*.java"):
        file_path = os.path.join(root, item)
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()
            
        new_content = content
        for p, r in patterns:
            if r"(\n.*)*" in p: # Handle multiline carefully if needed
                new_content = re.sub(p, r, new_content, flags=re.MULTILINE)
            else:
                new_content = re.sub(p, r, new_content)
                
        if new_content != content:
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(new_content)
                
print("Refactoring completed successfully.")
