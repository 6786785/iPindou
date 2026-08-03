const { chromium } = require("playwright");
const path = require("node:path");
const { pathToFileURL } = require("node:url");

const appUrl = pathToFileURL(path.resolve(__dirname, "..", "index-pro.html")).href;
const outputRoot = "C:\\Users\\19138\\.codex\\visualizations\\2026\\08\\02\\019fc2b4-9e79-7c02-9d39-0392e8958ac1";
const sizes = [
  [360, 640], [390, 844], [600, 960], [768, 1024], [1280, 720], [1920, 1080]
];

(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe" });
  const errors = [];
  for (const [width, height] of sizes) {
    const page = await browser.newPage({ viewport: { width, height }, deviceScaleFactor: 1 });
    page.on("pageerror", (error) => errors.push(`${width}x${height}: ${error.message}`));
    page.on("console", (message) => { if (message.type() === "error") errors.push(`${width}x${height}: ${message.text()}`); });
    await page.goto(appUrl);
    await page.waitForTimeout(180);
    await page.screenshot({ path: path.join(outputRoot, `v300-${width}x${height}.png`), fullPage: false });
    const overflow = await page.evaluate(() => ({ x: document.documentElement.scrollWidth - innerWidth, y: document.documentElement.scrollHeight - innerHeight }));
    if (overflow.x > 1 || overflow.y > 1) errors.push(`${width}x${height}: page overflow ${JSON.stringify(overflow)}`);
    if (height > width) {
      if (await page.locator(".mobile-bottom-nav").count()) errors.push(`${width}x${height}: legacy mobile nav remains in DOM`);
      if (!await page.locator(".panel").isVisible()) errors.push(`${width}x${height}: inspector not visible`);
    } else {
      const layout = await page.evaluate(() => {
        const bar = document.querySelector(".workspace-bar").getBoundingClientRect();
        const panel = document.querySelector(".panel").getBoundingClientRect();
        const head = document.querySelector(".inspector-head").getBoundingClientRect();
        const preview = document.querySelector(".preview-shell").getBoundingClientRect();
        return { barBottom: bar.bottom, panelTop: panel.top, panelRight: panel.right, headTop: head.top, previewLeft: preview.left };
      });
      if (Math.abs(layout.barBottom - layout.panelTop) > 1 || Math.abs(layout.panelTop - layout.headTop) > 1 || Math.abs(layout.panelRight - layout.previewLeft) > 1) errors.push(`${width}x${height}: landscape regions are not tightly joined ${JSON.stringify(layout)}`);
    }
    await page.close();
  }

  const darkPage = await browser.newPage({ viewport: { width: 390, height: 844 }, colorScheme: "dark", reducedMotion: "reduce" });
  darkPage.on("pageerror", (error) => errors.push(`dark/reduced: ${error.message}`));
  await darkPage.goto(appUrl);
  await darkPage.waitForTimeout(120);
  const darkTokens = await darkPage.evaluate(() => {
    const style = getComputedStyle(document.documentElement);
    return { bg: style.getPropertyValue("--bg").trim(), accent: style.getPropertyValue("--accent").trim() };
  });
  if (darkTokens.bg.toLowerCase() !== "#211f1b" || darkTokens.accent.toLowerCase() !== "#79aa9d") errors.push(`dark/reduced: theme tokens not applied ${JSON.stringify(darkTokens)}`);
  await darkPage.screenshot({ path: path.join(outputRoot, "v300-dark-reduced.png") });
  await darkPage.close();

  const page = await browser.newPage({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 1 });
  page.on("pageerror", (error) => errors.push(`flow: ${error.message}`));
  page.on("console", (message) => { if (message.type() === "error") errors.push(`flow: ${message.text()}`); });
  await page.goto(appUrl);
  await page.waitForTimeout(160);

  await page.locator('[data-panel-tab="basic"]').click();
  if (await page.locator(".panel").getAttribute("data-snap") !== "half") errors.push("flow: active tab changed inspector height unexpectedly");
  await page.locator('[data-panel-tab="colors"]').click();
  if (await page.locator(".panel").getAttribute("data-snap") !== "half") errors.push("flow: category did not preserve inspector height");
  if (!await page.locator("#portraitHistoryBtn").isVisible()) errors.push("flow: portrait history button is not visible");
  await page.locator("#portraitHistoryBtn").click();
  if (!await page.locator("#historyDialog").evaluate((el) => el.open)) errors.push("flow: portrait history button did not open history");
  await page.locator("#closeHistoryDialogBtn").click();
  const handleBox = await page.locator("#inspectorHandle").boundingBox();
  await page.evaluate(({ x, startY, endY }) => {
    const init = { bubbles: true, cancelable: true, pointerId: 77, pointerType: "touch", isPrimary: true, clientX: x };
    inspectorHandle.dispatchEvent(new PointerEvent("pointerdown", { ...init, clientY: startY }));
    window.dispatchEvent(new PointerEvent("pointermove", { ...init, clientY: endY }));
    window.dispatchEvent(new PointerEvent("pointerup", { ...init, clientY: endY }));
  }, { x: handleBox.x + handleBox.width / 2, startY: handleBox.y + handleBox.height / 2, endY: handleBox.y - 260 });
  await page.waitForTimeout(260);
  const draggedSnap = await page.locator(".panel").getAttribute("data-snap");
  if (draggedSnap !== "expanded") errors.push(`flow: upward drag did not expand inspector (ended ${draggedSnap})`);
  const freeHandleBox = await page.locator("#inspectorHandle").boundingBox();
  await page.evaluate(({ x, startY }) => {
    const init = { bubbles: true, cancelable: true, pointerId: 78, pointerType: "touch", isPrimary: true, clientX: x };
    inspectorHandle.dispatchEvent(new PointerEvent("pointerdown", { ...init, clientY: startY }));
  }, { x: freeHandleBox.x + freeHandleBox.width / 2, startY: freeHandleBox.y + freeHandleBox.height / 2 });
  await page.waitForTimeout(180);
  await page.evaluate(({ x, endY }) => {
    const init = { bubbles: true, cancelable: true, pointerId: 78, pointerType: "touch", isPrimary: true, clientX: x, clientY: endY };
    window.dispatchEvent(new PointerEvent("pointermove", init));
  }, { x: freeHandleBox.x + freeHandleBox.width / 2, endY: freeHandleBox.y + 95 });
  await page.waitForTimeout(180);
  await page.evaluate(({ x, endY }) => {
    const init = { bubbles: true, cancelable: true, pointerId: 78, pointerType: "touch", isPrimary: true, clientX: x, clientY: endY };
    window.dispatchEvent(new PointerEvent("pointermove", init));
    window.dispatchEvent(new PointerEvent("pointerup", init));
  }, { x: freeHandleBox.x + freeHandleBox.width / 2, endY: freeHandleBox.y + 95 });
  if (await page.locator(".panel").getAttribute("data-snap") !== "free") errors.push("flow: slow drag did not preserve a continuous free position");
  await page.locator('[data-panel-tab="output"]').click();
  if (await page.locator(".panel").getAttribute("data-snap") !== "free") errors.push("flow: category tap changed a free inspector position");
  const outputReachable = await page.locator(".panel").evaluate((el) => {
    el.scrollTop = el.scrollHeight;
    const group = document.querySelector('[data-panel-group="output"]');
    return group.getBoundingClientRect().bottom <= innerHeight + 1;
  });
  if (!outputReachable) errors.push("flow: output inspector bottom cannot be reached by scrolling");
  await page.locator("#moreActionsBtn").click();
  await page.screenshot({ path: path.join(outputRoot, "v300-portrait-menu.png") });
  await page.keyboard.press("Escape");
  await page.waitForTimeout(180);
  if (!await page.locator("#workspaceMenuPopover").isHidden()) errors.push("flow: Escape did not close More menu");

  await page.locator("#workspaceTitleButton").click();
  await page.locator("#renameProjectInput").fill("纸本测试图纸");
  await page.locator("#renameProjectForm button[type=submit]").click();
  if ((await page.locator("#workspaceTitle").textContent()).trim() !== "纸本测试图纸") errors.push("flow: rename failed");

  await page.locator("#moreActionsBtn").click();
  await page.locator("#blankProjectBtn").click();
  if (await page.locator("#appMessageDialog").evaluate((el) => el.open)) await page.locator("#appMessageConfirmBtn").click();
  await page.locator("#blankProjectName").fill("空白测试图纸");
  await page.locator("#blankProjectForm button[type=submit]").click();
  await page.waitForTimeout(220);
  if (!await page.locator('[data-panel-tab="edit"]').evaluate((el) => el.classList.contains("active"))) errors.push("flow: blank project did not enter Refine");
  if (!await page.locator("#drawingToolPanel").evaluate((el) => el.open)) errors.push("flow: blank project did not open Drawing");
  if (!await page.locator("#manualEditBtn").evaluate((el) => el.classList.contains("manual-editing"))) errors.push("flow: blank project did not start drawing mode");
  await page.locator("#manualColorCodeInput").fill("A1");
  await page.locator("#addManualColorCodeBtn").click();
  if (await page.locator("#manualColorCodeHint").textContent().then((text) => !text.includes("A1"))) errors.push("flow: drawing color code was not accepted");
  await page.locator("#portraitSmartBoardBtn").click();
  await page.waitForTimeout(160);
  if (await page.locator("#smartBoardView .focus-inspector").getAttribute("data-snap") !== "collapsed") errors.push("flow: smart board did not open collapsed");
  await page.locator('[data-smart-tab="colors"]').click();
  await page.screenshot({ path: path.join(outputRoot, "v300-smartboard.png") });
  await page.locator("#smartBoardLockBtn").click();
  if (!await page.locator("#smartBoardView").evaluate((el) => el.classList.contains("is-locked"))) errors.push("flow: smart board lock failed");
  await page.locator('[data-smart-tab="segments"]').click();
  if (!await page.locator('[data-smart-tab="segments"]').evaluate((el) => el.classList.contains("active"))) errors.push("flow: segments tab is unavailable while locked");
  if (await page.locator("#smartSegmentList button").first().isDisabled()) errors.push("flow: segment selection is disabled while locked");
  await page.locator('[data-smart-tab="colors"]').click();
  if (!await page.locator('[data-smart-tab="colors"]').evaluate((el) => el.classList.contains("active"))) errors.push("flow: colors tab is unavailable while locked");
  await page.locator("#smartBoardLockBtn").dispatchEvent("pointerdown", { pointerType: "touch", pointerId: 91, clientX: 360, clientY: 28 });
  await page.waitForTimeout(850);
  await page.locator("#smartBoardLockBtn").dispatchEvent("pointerup", { pointerType: "touch", pointerId: 91, clientX: 360, clientY: 28 });
  await page.locator("#smartBoardBackBtn").click();

  await page.locator("#moreActionsBtn").click();
  await page.locator("#uiSettingsBtn").click();
  const themeExpanded = page.locator("#themeSetting").locator("..");
  await themeExpanded.locator(".expandable-select-toggle").click();
  if (await themeExpanded.locator(".expandable-select-options").isHidden()) errors.push("flow: theme options did not expand inline");
  await page.evaluate(() => {
    themeSetting.value = "light";
    themeSetting.dispatchEvent(new Event("change", { bubbles: true }));
    document.querySelector("#uiSettingsForm button[type=submit]").click();
  });
  if (await page.locator("html").getAttribute("data-theme") !== "light") errors.push("flow: explicit light theme was not applied");

  const png = Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0kAAAAFElEQVR42mNkYPj/n4GBgYGJAQoAHgQCAc3FQOYAAAAASUVORK5CYII=", "base64");
  await page.locator("#imageInput").setInputFiles({ name: "sample.png", mimeType: "image/png", buffer: png });
  await page.waitForTimeout(500);
  await page.locator('[data-panel-tab="basic"]').click();
  await page.evaluate(() => setInspectorSnap("expanded", false));
  await page.locator("#openAdvancedCropBtn").click();
  await page.waitForTimeout(120);
  if (!await page.locator("#advancedCropConfirmBtn").isDisabled()) errors.push("flow: crop confirm should start disabled");
  await page.locator('[data-crop-tab="preview"]').click();
  await page.locator("#advancedCropGenerateBtn").click();
  await page.waitForTimeout(500);
  if (await page.locator("#advancedCropConfirmBtn").isDisabled()) errors.push("flow: crop confirm did not enable after preview");
  await page.screenshot({ path: path.join(outputRoot, "v300-crop.png") });
  await page.locator("#advancedCropConfirmBtn").click();
  await page.waitForTimeout(250);
  if (await page.locator("#advancedCropView").evaluate((el) => el.classList.contains("open"))) errors.push("flow: crop confirm did not close focused mode");

  await page.close();
  await browser.close();
  if (errors.length) {
    console.error(errors.join("\n"));
    process.exitCode = 1;
  } else {
    console.log("6 responsive sizes, dark/reduced-motion, and focused-mode flows passed without console errors");
  }
})();
