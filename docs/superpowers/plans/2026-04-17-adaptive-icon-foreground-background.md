# Adaptive Icon Foreground Background Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce review-ready adaptive icon foreground and background assets derived from `icon_concept.svg`, plus a merged preview image, without integrating them into the Android app yet.

**Architecture:** Treat `icon_concept.svg` as the visual source, but stop using it as a single merged launcher graphic. Extract a transparent foreground containing the photo and lock, create a lightweight background layer, and combine them only for review previews. Keep all outputs under the worktree asset directory so later Android integration can consume approved assets without redoing the visual work.

**Tech Stack:** SVG, PNG export, repository asset files, Windows PowerShell, Microsoft Edge headless screenshot export

---

### Task 1: Prepare The Adaptive Icon Asset Workspace

**Files:**
- Read: `/home/dairidong/code/nomedia-switcher/icon_concept.svg`
- Create: `/home/dairidong/code/nomedia-switcher/.worktrees/android-app-v1/assets/app-icon/foreground.svg`
- Create: `/home/dairidong/code/nomedia-switcher/.worktrees/android-app-v1/assets/app-icon/background.svg`

- [ ] **Step 1: Inspect the source icon structure**

Run:
```bash
sed -n '1,260p' /home/dairidong/code/nomedia-switcher/icon_concept.svg
```
Expected: the source still contains the rounded-square plate, photo group, and lock group

- [ ] **Step 2: Ensure the worktree asset directory exists**

Run:
```bash
mkdir -p /home/dairidong/code/nomedia-switcher/.worktrees/android-app-v1/assets/app-icon
```
Expected: the worktree asset directory exists

- [ ] **Step 3: Define foreground and background ownership**

Create a note in the implementation context only:
- foreground owns photo + lock, transparent outside
- background owns only a quiet light background layer

Expected: no ambiguity remains about what belongs in each output

- [ ] **Step 4: Do not touch Android resources yet**

Verify that this task does not modify:
- `app/src/main/res/`
- `AndroidManifest.xml`

Expected: this task only prepares the asset workspace

### Task 2: Create The Foreground Layer

**Files:**
- Create: `/home/dairidong/code/nomedia-switcher/.worktrees/android-app-v1/assets/app-icon/foreground.svg`
- Create: `/home/dairidong/code/nomedia-switcher/.worktrees/android-app-v1/assets/app-icon/foreground.png`

- [ ] **Step 1: Write the foreground SVG**

Build a transparent SVG that contains:
- the photo card
- the lock

Remove:
- the old full rounded-square base plate

Keep:
- lock visibility
- photo dominance
- gradients and shadows where feasible

- [ ] **Step 2: Correct foreground composition for adaptive icon use**

Adjust the foreground composition so:
- outer spacing is balanced
- the visible subject is optically centered
- the lock still reads clearly

Do not redesign shapes or style.

- [ ] **Step 3: Export a foreground PNG preview**

Run:
```bash
powershell.exe -NoProfile -Command '$src = "\\wsl.localhost\Ubuntu\home\dairidong\code\nomedia-switcher\.worktrees\android-app-v1\assets\app-icon\foreground.svg"; $svg = Join-Path $env:TEMP "foreground.svg"; $png = Join-Path $env:TEMP "foreground.png"; Copy-Item -LiteralPath $src -Destination $svg -Force; & "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" --headless --disable-gpu --window-size=1024,1024 --screenshot=$png ("file:///" + $svg.Replace("\","/")); Start-Sleep -Seconds 2; Get-Item $png | Select-Object FullName,Length,LastWriteTime | Format-Table -AutoSize'
```
Expected: `foreground.png` exists in Windows temp

- [ ] **Step 4: Copy the foreground PNG back into the worktree**

Run:
```bash
cp /mnt/c/Users/dairidong/AppData/Local/Temp/foreground.png /home/dairidong/code/nomedia-switcher/.worktrees/android-app-v1/assets/app-icon/foreground.png
```
Expected: `foreground.png` exists in the worktree asset directory

### Task 3: Create The Background Layer

**Files:**
- Create: `/home/dairidong/code/nomedia-switcher/.worktrees/android-app-v1/assets/app-icon/background.svg`
- Create: `/home/dairidong/code/nomedia-switcher/.worktrees/android-app-v1/assets/app-icon/background.png`

- [ ] **Step 1: Write the background SVG**

Create a simple background layer using:
- a plain light fill, or
- a very soft light gradient

Do not keep:
- the old large rounded-square plate from the previous mockup

- [ ] **Step 2: Keep the background visually quiet**

Check that the background does not compete with the foreground:
- no strong edge treatment
- no heavy shadow
- no busy framing

Expected: the background supports rather than dominates

- [ ] **Step 3: Export a background PNG preview**

Run:
```bash
powershell.exe -NoProfile -Command '$src = "\\wsl.localhost\Ubuntu\home\dairidong\code\nomedia-switcher\.worktrees\android-app-v1\assets\app-icon\background.svg"; $svg = Join-Path $env:TEMP "background.svg"; $png = Join-Path $env:TEMP "background.png"; Copy-Item -LiteralPath $src -Destination $svg -Force; & "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" --headless --disable-gpu --window-size=1024,1024 --screenshot=$png ("file:///" + $svg.Replace("\","/")); Start-Sleep -Seconds 2; Get-Item $png | Select-Object FullName,Length,LastWriteTime | Format-Table -AutoSize'
```
Expected: `background.png` exists in Windows temp

- [ ] **Step 4: Copy the background PNG back into the worktree**

Run:
```bash
cp /mnt/c/Users/dairidong/AppData/Local/Temp/background.png /home/dairidong/code/nomedia-switcher/.worktrees/android-app-v1/assets/app-icon/background.png
```
Expected: `background.png` exists in the worktree asset directory

### Task 4: Create And Verify The Merged Preview

**Files:**
- Verify: `/home/dairidong/code/nomedia-switcher/.worktrees/android-app-v1/assets/app-icon/foreground.svg`
- Verify: `/home/dairidong/code/nomedia-switcher/.worktrees/android-app-v1/assets/app-icon/background.svg`
- Create: `/home/dairidong/code/nomedia-switcher/.worktrees/android-app-v1/assets/app-icon/icon_preview.png`

- [ ] **Step 1: Create a merged preview SVG or HTML composition for export**

Create the smallest possible composition artifact needed to place:
- `background.svg`
- `foreground.svg`

together for preview only

Expected: there is a stable way to render the two layers together without yet creating Android launcher resources

- [ ] **Step 2: Export the merged preview PNG**

Use the same Windows Edge export path as previous tasks.

Expected: `icon_preview.png` exists and shows foreground over background

- [ ] **Step 3: Copy the merged preview into the worktree asset directory**

Place the review image at:
- `/home/dairidong/code/nomedia-switcher/.worktrees/android-app-v1/assets/app-icon/icon_preview.png`

- [ ] **Step 4: Visually verify the review outputs**

Review all three PNGs:
- `foreground.png`
- `background.png`
- `icon_preview.png`

Check:
- the lock is still present and readable
- the photo remains the dominant subject
- the foreground spacing is balanced
- the background stays quiet
- the merged preview feels suitable for future launcher integration

Expected: the assets are ready for user review

### Task 5: Stop Before Android Integration

**Files:**
- Verify only

- [ ] **Step 1: Confirm no app resources were changed**

Run:
```bash
git -C /home/dairidong/code/nomedia-switcher/.worktrees/android-app-v1 status --short app/src/main/AndroidManifest.xml app/src/main/res
```
Expected: no new adaptive launcher integration has been done in this round

- [ ] **Step 2: Verify the final asset set exists**

Run:
```bash
ls -l /home/dairidong/code/nomedia-switcher/.worktrees/android-app-v1/assets/app-icon
```
Expected: the directory contains:
- `foreground.svg`
- `foreground.png`
- `background.svg`
- `background.png`
- `icon_preview.png`

- [ ] **Step 3: Commit the review asset work**

Run:
```bash
git -C /home/dairidong/code/nomedia-switcher/.worktrees/android-app-v1 add assets/app-icon docs/superpowers/specs/2026-04-17-adaptive-icon-foreground-background-design.md docs/superpowers/plans/2026-04-17-adaptive-icon-foreground-background.md
git -C /home/dairidong/code/nomedia-switcher/.worktrees/android-app-v1 commit -m "design: prepare adaptive icon foreground and background assets"
```
Expected: a commit is created with reviewable adaptive icon assets only
