---
description: How to remove a file from git history (e.g., large files blocking push)
---

# Remove File from Git History

If you accidentally committed a large file (like a `.hprof` heap dump) and need to remove it from the entire git history to fix push errors:

## 1. Identify the File
Find the exact path of the file if needed:
```bash
git rev-list --objects --all | grep "filename"
```

## 2. Rewrite History
Use `git filter-branch` to remove the file from all commits.
Replace `YOUR_FILE_NAME` with the actual file path (e.g., `java_pid18224.hprof`).

```bash
git filter-branch --force --index-filter "git rm --cached --ignore-unmatch YOUR_FILE_NAME" --prune-empty --tag-name-filter cat -- --all
```

## 3. Push Changes
Force push the rewritten history to the remote repository.

```bash
git push origin main --force
```

> **Note**: This command rewrites history. Anyone else working on this branch will need to re-clone or reset their local branch.
