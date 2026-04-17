# App Icon Merged PNG Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Correct the whitespace and composition of `icon_concept.svg`, then produce an editable merged SVG master and a high-resolution PNG for user review.

**Architecture:** Treat `/home/dairidong/code/nomedia-switcher/icon_concept.svg` as the only source design. Create a corrected merged master under `assets/app-icon/`, then export a PNG from that corrected master using Windows Edge headless rendering so gradients and drop shadows survive intact.

**Tech Stack:** SVG, repository asset files, Windows PowerShell, Microsoft Edge headless screenshot export

---

### Task 1: Prepare The Icon Asset Workspace

**Files:**
- Read: `/home/dairidong/code/nomedia-switcher/icon_concept.svg`
- Create: `/home/dairidong/code/nomedia-switcher/assets/app-icon/`
- Create: `/home/dairidong/code/nomedia-switcher/assets/app-icon/icon_merged.svg`

- [ ] **Step 1: Inspect the source SVG structure**

Read the current source and confirm it still contains:
- one rounded-square background plate
- one photo group
- one lock group

Run:
```bash
sed -n '1,260p' /home/dairidong/code/nomedia-switcher/icon_concept.svg
```
Expected: the SVG contains the current gradient, shadow, photo, and lock definitions

- [ ] **Step 2: Create the asset output directory**

Run:
```bash
mkdir -p /home/dairidong/code/nomedia-switcher/assets/app-icon
```
Expected: `assets/app-icon/` exists

- [ ] **Step 3: Copy the current SVG into the working merged-master path**

Run:
```bash
cp /home/dairidong/code/nomedia-switcher/icon_concept.svg /home/dairidong/code/nomedia-switcher/assets/app-icon/icon_merged.svg
```
Expected: `assets/app-icon/icon_merged.svg` exists and matches the source file

- [ ] **Step 4: Export a baseline preview to capture the current defect**

Run:
```bash
powershell.exe -NoProfile -Command '$src = "\\wsl.localhost\Ubuntu\home\dairidong\code\nomedia-switcher\assets\app-icon\icon_merged.svg"; $svg = Join-Path $env:TEMP "icon_merged-baseline.svg"; $png = Join-Path $env:TEMP "icon_merged-baseline.png"; Copy-Item -LiteralPath $src -Destination $svg -Force; & "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" --headless --disable-gpu --window-size=1024,1024 --screenshot=$png ("file:///" + $svg.Replace("\","/")); Start-Sleep -Seconds 2; Get-Item $png | Select-Object FullName,Length,LastWriteTime | Format-Table -AutoSize'
```
Expected: a baseline PNG exists in Windows temp and still shows uneven outer whitespace

### Task 2: Correct The SVG Composition

**Files:**
- Modify: `/home/dairidong/code/nomedia-switcher/assets/app-icon/icon_merged.svg`

- [ ] **Step 1: Identify which transforms control the visible imbalance**

Read the copied merged SVG and locate:
- the photo group transform
- the lock group transform
- any background geometry that affects perceived margins

Run:
```bash
sed -n '1,260p' /home/dairidong/code/nomedia-switcher/assets/app-icon/icon_merged.svg
```
Expected: the transform values for the photo and lock groups are visible and ready to edit

- [ ] **Step 2: Adjust the merged SVG minimally to balance whitespace**

Edit only the composition-related values needed to restore the intended design balance:
- keep the 512x512 artboard
- preserve the current shapes, gradients, and shadows
- correct the relative placement so the outer whitespace no longer feels obviously uneven

Do not:
- redesign the lock
- simplify the image
- change the overall visual language

- [ ] **Step 3: Re-read the edited SVG for accidental design drift**

Run:
```bash
sed -n '1,260p' /home/dairidong/code/nomedia-switcher/assets/app-icon/icon_merged.svg
```
Expected: only composition-related values changed; the original visual components are still present

### Task 3: Export The Review PNG

**Files:**
- Read: `/home/dairidong/code/nomedia-switcher/assets/app-icon/icon_merged.svg`
- Create: `/home/dairidong/code/nomedia-switcher/assets/app-icon/icon_full.png`

- [ ] **Step 1: Export the corrected SVG to a Windows-side PNG**

Run:
```bash
powershell.exe -NoProfile -Command '$src = "\\wsl.localhost\Ubuntu\home\dairidong\code\nomedia-switcher\assets\app-icon\icon_merged.svg"; $svg = Join-Path $env:TEMP "icon_merged-final.svg"; $png = Join-Path $env:TEMP "icon_full.png"; Copy-Item -LiteralPath $src -Destination $svg -Force; & "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" --headless --disable-gpu --window-size=1024,1024 --screenshot=$png ("file:///" + $svg.Replace("\","/")); Start-Sleep -Seconds 2; Get-Item $png | Select-Object FullName,Length,LastWriteTime | Format-Table -AutoSize'
```
Expected: `icon_full.png` is created in Windows temp

- [ ] **Step 2: Copy the exported PNG back into the repository asset path**

Run:
```bash
cp /mnt/c/Users/dairidong/AppData/Local/Temp/icon_full.png /home/dairidong/code/nomedia-switcher/assets/app-icon/icon_full.png
```
Expected: `/home/dairidong/code/nomedia-switcher/assets/app-icon/icon_full.png` exists

- [ ] **Step 3: Verify the repository assets exist**

Run:
```bash
ls -l /home/dairidong/code/nomedia-switcher/assets/app-icon/icon_merged.svg /home/dairidong/code/nomedia-switcher/assets/app-icon/icon_full.png
```
Expected: both files exist and have recent timestamps

### Task 4: Visual Verification And Handoff

**Files:**
- Verify: `/home/dairidong/code/nomedia-switcher/assets/app-icon/icon_merged.svg`
- Verify: `/home/dairidong/code/nomedia-switcher/assets/app-icon/icon_full.png`

- [ ] **Step 1: Open the final PNG for visual review**

Run:
```bash
```
Use the image viewer tool on:
`/home/dairidong/code/nomedia-switcher/assets/app-icon/icon_full.png`

Expected: the lock, photo, and background plate are all intact and the outer whitespace looks materially more balanced than the baseline export

- [ ] **Step 2: Compare the final result against the approved design requirements**

Check:
- the lock remains present and legible
- the photo remains the dominant element
- the base plate shape and softness remain intact
- gradients and shadows are preserved
- the whitespace no longer draws attention as a defect

Expected: the final PNG is suitable for user review

- [ ] **Step 3: Stop before Android integration**

Do not generate launcher resources yet.

Expected: the task ends with reviewable design assets only:
- `assets/app-icon/icon_merged.svg`
- `assets/app-icon/icon_full.png`

- [ ] **Step 4: Commit the asset-preparation work**

Run:
```bash
git add assets/app-icon/icon_merged.svg assets/app-icon/icon_full.png docs/superpowers/specs/2026-04-16-app-icon-merged-png-design.md docs/superpowers/plans/2026-04-17-app-icon-merged-png.md
git commit -m "design: prepare merged app icon master and png review asset"
```
Expected: a commit is created with the corrected icon assets and planning docs
