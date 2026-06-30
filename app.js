const dayNames = ["週一", "週二", "週三", "週四", "週五", "週六", "週日"];
const categoryColors = {
  "工作": "#167a6b",
  "學習": "#426c9f",
  "休息": "#7c6b58",
  "運動": "#c8942d",
  "社交": "#9a5b8f",
  "家務": "#697c47",
  "娛樂": "#d9634e",
  "其他": "#5d6572"
};

const state = {
  weekStart: startOfWeek(new Date()),
  entries: loadEntries(),
  draft: null,
  editingId: null,
  timer: {
    startedAt: null,
    intervalId: null
  }
};

const els = {
  weekLabel: document.querySelector("#weekLabel"),
  prevWeek: document.querySelector("#prevWeek"),
  nextWeek: document.querySelector("#nextWeek"),
  todayButton: document.querySelector("#todayButton"),
  focusTimer: document.querySelector("#focusTimer"),
  timerStatus: document.querySelector("#timerStatus"),
  timerElapsed: document.querySelector("#timerElapsed"),
  timerStart: document.querySelector("#timerStart"),
  timerEnd: document.querySelector("#timerEnd"),
  focusOverlay: document.querySelector("#focusOverlay"),
  focusStartTime: document.querySelector("#focusStartTime"),
  focusEndTime: document.querySelector("#focusEndTime"),
  focusElapsedTime: document.querySelector("#focusElapsedTime"),
  focusEndControl: document.querySelector("#focusEndControl"),
  calendar: document.querySelector("#calendar"),
  dayStrip: document.querySelector("#dayStrip"),
  timeAxis: document.querySelector("#timeAxis"),
  dayGrid: document.querySelector("#dayGrid"),
  weekTotal: document.querySelector("#weekTotal"),
  topActivity: document.querySelector("#topActivity"),
  eventModal: document.querySelector("#eventModal"),
  eventForm: document.querySelector("#eventForm"),
  modalTitle: document.querySelector("#modalTitle"),
  activityInput: document.querySelector("#activityInput"),
  startInput: document.querySelector("#startInput"),
  endInput: document.querySelector("#endInput"),
  categoryInput: document.querySelector("#categoryInput"),
  noteInput: document.querySelector("#noteInput"),
  deleteEvent: document.querySelector("#deleteEvent"),
  closeModal: document.querySelector("#closeModal"),
  cancelModal: document.querySelector("#cancelModal"),
  exportButton: document.querySelector("#exportButton"),
  exportModal: document.querySelector("#exportModal"),
  closeExport: document.querySelector("#closeExport"),
  downloadJson: document.querySelector("#downloadJson"),
  downloadCsv: document.querySelector("#downloadCsv"),
  copyPrompt: document.querySelector("#copyPrompt"),
  shareText: document.querySelector("#shareText"),
  exportPreview: document.querySelector("#exportPreview"),
  toast: document.querySelector("#toast")
};

init();

function init() {
  renderTimeAxis();
  bindEvents();
  render();
  updateTimerDisplay();
  requestAnimationFrame(scrollToMorning);
}

function bindEvents() {
  els.prevWeek.addEventListener("click", () => shiftWeek(-7));
  els.nextWeek.addEventListener("click", () => shiftWeek(7));
  els.todayButton.addEventListener("click", () => {
    state.weekStart = startOfWeek(new Date());
    render();
    scrollToMorning();
  });
  els.weekLabel.addEventListener("click", scrollToMorning);
  els.timerStart.addEventListener("click", startFocusTimer);
  els.timerEnd.addEventListener("click", endFocusTimer);
  els.focusEndControl.addEventListener("click", endFocusTimer);
  els.closeModal.addEventListener("click", closeEventModal);
  els.cancelModal.addEventListener("click", closeEventModal);
  els.eventModal.addEventListener("click", (event) => {
    if (event.target === els.eventModal) closeEventModal();
  });
  els.eventForm.addEventListener("submit", saveEvent);
  els.deleteEvent.addEventListener("click", deleteCurrentEvent);
  els.exportButton.addEventListener("click", openExportModal);
  els.closeExport.addEventListener("click", closeExportModal);
  els.exportModal.addEventListener("click", (event) => {
    if (event.target === els.exportModal) closeExportModal();
  });
  els.downloadJson.addEventListener("click", downloadJson);
  els.downloadCsv.addEventListener("click", downloadCsv);
  els.copyPrompt.addEventListener("click", copyPrompt);
  els.shareText.addEventListener("click", shareText);
}

function render() {
  renderDayStrip();
  renderWeekLabel();
  renderGrid();
  renderSummary();
}

function renderDayStrip() {
  els.dayStrip.innerHTML = "";
  const todayKey = dateKey(new Date());
  getWeekDates().forEach((date, index) => {
    const tab = document.createElement("div");
    tab.className = `day-tab${dateKey(date) === todayKey ? " today" : ""}`;
    tab.innerHTML = `<strong>${dayNames[index]}</strong><span>${date.getDate()}</span>`;
    els.dayStrip.appendChild(tab);
  });
}

function renderWeekLabel() {
  const dates = getWeekDates();
  els.weekLabel.textContent = `${formatMonthDay(dates[0])} - ${formatMonthDay(dates[6])}`;
}

function renderTimeAxis() {
  els.timeAxis.innerHTML = "";
  for (let hour = 0; hour <= 24; hour += 2) {
    const label = document.createElement("div");
    label.className = "hour-label";
    label.style.top = `${hourToY(hour)}px`;
    label.textContent = `${String(hour).padStart(2, "0")}:00`;
    els.timeAxis.appendChild(label);
  }
}

function renderGrid() {
  els.dayGrid.innerHTML = "";
  getWeekDates().forEach((date) => {
    const column = document.createElement("div");
    column.className = "day-column";
    column.dataset.date = dateKey(date);
    els.dayGrid.appendChild(column);

    const rail = document.createElement("button");
    rail.type = "button";
    rail.className = "day-drag-rail";
    rail.setAttribute("aria-label", "Create time block");
    rail.addEventListener("pointerdown", startDraft);
    column.appendChild(rail);

    state.entries
      .filter((entry) => entry.date === column.dataset.date)
      .sort((a, b) => a.start - b.start)
      .forEach((entry) => column.appendChild(renderBlock(entry)));
  });
}

function renderBlock(entry) {
  const block = document.createElement("button");
  block.type = "button";
  block.className = "time-block";
  block.style.top = `${minutesToY(entry.start)}px`;
  block.style.height = `${Math.max(minutesToY(entry.end - entry.start), 28)}px`;
  block.style.background = categoryColors[entry.category] || categoryColors["其他"];
  block.innerHTML = `<strong>${escapeHtml(entry.activity || entry.category)}</strong><span>${formatTime(entry.start)}-${formatTime(entry.end)}</span>`;
  block.addEventListener("pointerdown", (event) => event.stopPropagation());
  block.addEventListener("click", () => openEventModal(entry));
  return block;
}

function renderSummary() {
  const weekKeys = new Set(getWeekDates().map(dateKey));
  const weekEntries = state.entries.filter((entry) => weekKeys.has(entry.date));
  const totalMinutes = weekEntries.reduce((sum, entry) => sum + entry.end - entry.start, 0);
  const buckets = new Map();

  weekEntries.forEach((entry) => {
    const key = entry.category || "其他";
    buckets.set(key, (buckets.get(key) || 0) + entry.end - entry.start);
  });

  const top = [...buckets.entries()].sort((a, b) => b[1] - a[1])[0];
  els.weekTotal.textContent = formatDuration(totalMinutes);
  els.topActivity.textContent = top ? `${top[0]} ${formatDuration(top[1])}` : "尚無資料";
}

function startDraft(event) {
  if (event.button !== 0 && event.pointerType === "mouse") return;
  if (event.target.closest(".time-block")) return;
  if (state.draft) return;
  const column = event.currentTarget.closest(".day-column");
  if (!column) return;

  beginDraft({
    column,
    control: event.currentTarget,
    pointerId: event.pointerId,
    clientY: event.clientY,
    preventDefault: () => event.preventDefault()
  });
}

function beginDraft({ column, control = column, pointerId, clientY, listenersAttached = false, preventDefault }) {
  const startY = pointerYInColumnClientY(clientY, column);
  const draftEl = document.createElement("div");
  draftEl.className = "draft-block";
  column.appendChild(draftEl);

  state.draft = {
    column,
    control,
    element: draftEl,
    pointerId,
    date: column.dataset.date,
    start: snapMinutes(yToMinutes(startY)),
    end: snapMinutes(yToMinutes(startY + 30))
  };

  preventDefault();
  els.calendar?.classList.add("is-drafting");
  control.setPointerCapture(pointerId);
  updateDraftBlock();
  if (!listenersAttached) {
    control.addEventListener("pointermove", handleDraftPointerMove);
    control.addEventListener("pointerup", handleDraftPointerUp);
    control.addEventListener("pointercancel", handleDraftPointerCancel);
  }
}

function handleDraftPointerMove(event) {
  moveDraft(event);
}

function handleDraftPointerUp(event) {
  finishDraft(event);
}

function handleDraftPointerCancel() {
  cancelDraft();
}

function moveDraft(event) {
  if (!state.draft) return;
  event.preventDefault();
  state.draft.end = snapMinutes(yToMinutes(pointerYInColumn(event, state.draft.column)));
  updateDraftBlock();
}

function finishDraft(event) {
  if (!state.draft) return;
  moveDraft(event);
  const draft = normalizeDraft(state.draft);
  cleanupDraftListeners();
  state.draft.element.remove();
  state.draft = null;

  if (draft.end - draft.start < 15) return;
  openEventModal({
    id: null,
    date: draft.date,
    start: draft.start,
    end: draft.end,
    activity: "",
    category: "工作",
    note: ""
  });
}

function cancelDraft() {
  if (!state.draft) return;
  cleanupDraftListeners();
  state.draft.element.remove();
  state.draft = null;
}

function cleanupDraftListeners() {
  const { column, control, pointerId } = state.draft;
  const target = control || column;
  target.removeEventListener("pointermove", handleDraftPointerMove);
  target.removeEventListener("pointerup", handleDraftPointerUp);
  target.removeEventListener("pointercancel", handleDraftPointerCancel);
  els.calendar?.classList.remove("is-drafting");
  if (target.hasPointerCapture?.(pointerId)) {
    target.releasePointerCapture(pointerId);
  }
}

function pointerYInColumn(event, column) {
  return pointerYInColumnClientY(event.clientY, column);
}

function pointerYInColumnClientY(clientY, column) {
  const rect = column.getBoundingClientRect();
  return clamp(clientY - rect.top, 0, hourToY(24));
}

function updateDraftBlock() {
  const draft = normalizeDraft(state.draft);
  state.draft.element.style.top = `${minutesToY(draft.start)}px`;
  state.draft.element.style.height = `${Math.max(minutesToY(draft.end - draft.start), 24)}px`;
}

function normalizeDraft(draft) {
  const start = Math.min(draft.start, draft.end);
  const end = Math.max(draft.start, draft.end);
  return {
    date: draft.date,
    start: clamp(start, 0, 1425),
    end: clamp(end === start ? end + 30 : end, 15, 1440)
  };
}

function startFocusTimer() {
  if (state.timer.startedAt) return;
  state.timer.startedAt = new Date();
  state.timer.intervalId = setInterval(updateTimerDisplay, 1000);
  els.focusTimer.classList.add("running");
  els.focusOverlay.hidden = false;
  document.body.classList.add("focus-mode");
  els.timerStart.disabled = true;
  els.timerEnd.disabled = false;
  updateTimerDisplay();
  showToast("專注開始");
}

function endFocusTimer() {
  if (!state.timer.startedAt) return;
  const startedAt = state.timer.startedAt;
  const endedAt = new Date();
  const range = timerEntryRange(startedAt, endedAt);

  clearInterval(state.timer.intervalId);
  state.timer.startedAt = null;
  state.timer.intervalId = null;
  els.focusTimer.classList.remove("running");
  els.focusEndTime.textContent = formatClock(endedAt);
  els.focusOverlay.hidden = true;
  document.body.classList.remove("focus-mode");
  els.timerStart.disabled = false;
  els.timerEnd.disabled = true;
  updateTimerDisplay();

  const date = dateKey(startedAt);
  state.weekStart = startOfWeek(startedAt);
  render();
  scrollToTime(range.start);
  openEventModal({
    id: null,
    date,
    start: range.start,
    end: range.end,
    activity: "",
    category: "學習",
    note: `專注計時 ${formatDuration(range.duration)}`
  });
}

function updateTimerDisplay() {
  if (!state.timer.startedAt) {
    els.timerStatus.textContent = "專注計時";
    els.timerElapsed.textContent = "00:00:00";
    els.focusStartTime.textContent = "--:--";
    els.focusEndTime.textContent = "點一下結束";
    els.focusElapsedTime.textContent = "00:00:00";
    return;
  }

  const elapsedSeconds = Math.max(0, Math.floor((Date.now() - state.timer.startedAt.getTime()) / 1000));
  const elapsedText = formatElapsed(elapsedSeconds);
  els.timerStatus.textContent = `開始於 ${formatClock(state.timer.startedAt)}`;
  els.timerElapsed.textContent = elapsedText;
  els.focusStartTime.textContent = formatClock(state.timer.startedAt);
  els.focusEndTime.textContent = "點一下結束";
  els.focusElapsedTime.textContent = elapsedText;
}

function openEventModal(entry) {
  state.editingId = entry.id;
  els.modalTitle.textContent = entry.id ? "編輯時間塊" : "新增時間塊";
  els.activityInput.value = entry.activity || "";
  els.startInput.value = formatTime(entry.start);
  els.endInput.value = formatTime(entry.end);
  els.categoryInput.value = entry.category || "其他";
  els.noteInput.value = entry.note || "";
  els.eventForm.dataset.date = entry.date;
  els.deleteEvent.hidden = !entry.id;
  els.eventModal.hidden = false;
  requestAnimationFrame(() => els.activityInput.focus());
}

function closeEventModal() {
  els.eventModal.hidden = true;
  state.editingId = null;
  els.eventForm.reset();
}

function saveEvent(event) {
  event.preventDefault();
  const date = els.eventForm.dataset.date;
  const start = timeToMinutes(els.startInput.value);
  const end = timeToMinutes(els.endInput.value);

  if (!els.activityInput.value.trim()) {
    showToast("請輸入這段時間做了什麼");
    return;
  }

  if (end <= start) {
    showToast("結束時間要晚於開始時間");
    return;
  }

  const entry = {
    id: state.editingId || crypto.randomUUID(),
    date,
    start,
    end,
    activity: els.activityInput.value.trim(),
    category: els.categoryInput.value,
    note: els.noteInput.value.trim(),
    updatedAt: new Date().toISOString()
  };

  const index = state.entries.findIndex((item) => item.id === entry.id);
  if (index >= 0) {
    state.entries[index] = entry;
  } else {
    state.entries.push(entry);
  }

  persist();
  closeEventModal();
  render();
  showToast("已儲存");
}

function deleteCurrentEvent() {
  if (!state.editingId) return;
  state.entries = state.entries.filter((entry) => entry.id !== state.editingId);
  persist();
  closeEventModal();
  render();
  showToast("已刪除");
}

function openExportModal() {
  els.exportPreview.value = buildAnalysisText();
  els.exportModal.hidden = false;
}

function closeExportModal() {
  els.exportModal.hidden = true;
}

function downloadJson() {
  const payload = JSON.stringify({
    exportedAt: new Date().toISOString(),
    entries: sortedEntries()
  }, null, 2);
  downloadFile("time-blocks.json", payload, "application/json");
}

function downloadCsv() {
  const rows = [
    ["date", "start", "end", "duration_minutes", "category", "activity", "note"],
    ...sortedEntries().map((entry) => [
      entry.date,
      formatTime(entry.start),
      formatTime(entry.end),
      entry.end - entry.start,
      entry.category,
      entry.activity,
      entry.note
    ])
  ];
  const csv = rows.map((row) => row.map(csvCell).join(",")).join("\n");
  downloadFile("time-blocks.csv", csv, "text/csv;charset=utf-8");
}

async function copyPrompt() {
  const text = buildAnalysisText();
  try {
    await navigator.clipboard.writeText(text);
    showToast("已複製，可貼到 GPT 或 Telegram");
  } catch {
    els.exportPreview.select();
    document.execCommand("copy");
    showToast("已複製");
  }
}

async function shareText() {
  const text = buildAnalysisText();
  if (navigator.share) {
    await navigator.share({ title: "我的時間使用摘要", text });
  } else {
    await copyPrompt();
  }
}

function buildAnalysisText() {
  const entries = sortedEntries();
  const total = entries.reduce((sum, entry) => sum + entry.end - entry.start, 0);
  const byCategory = summarizeBy(entries, "category");
  const byActivity = summarizeBy(entries, "activity").slice(0, 8);
  const lines = [
    "請分析我的時間使用模式，找出主要時間流向、可能的浪費區塊、作息規律，以及下週可以改善的 3 個具體建議。",
    "",
    `資料筆數：${entries.length}`,
    `總紀錄時間：${formatDuration(total)}`,
    "",
    "分類統計：",
    ...byCategory.map(([name, minutes]) => `- ${name}: ${formatDuration(minutes)}`),
    "",
    "活動統計：",
    ...byActivity.map(([name, minutes]) => `- ${name}: ${formatDuration(minutes)}`),
    "",
    "原始紀錄：",
    ...entries.map((entry) => `- ${entry.date} ${formatTime(entry.start)}-${formatTime(entry.end)} ${entry.category}｜${entry.activity}${entry.note ? `｜${entry.note}` : ""}`)
  ];
  return lines.join("\n");
}

function summarizeBy(entries, key) {
  const buckets = new Map();
  entries.forEach((entry) => {
    const name = entry[key] || "未填";
    buckets.set(name, (buckets.get(name) || 0) + entry.end - entry.start);
  });
  return [...buckets.entries()].sort((a, b) => b[1] - a[1]);
}

function sortedEntries() {
  return [...state.entries].sort((a, b) => `${a.date}-${a.start}`.localeCompare(`${b.date}-${b.start}`));
}

function shiftWeek(days) {
  const next = new Date(state.weekStart);
  next.setDate(next.getDate() + days);
  state.weekStart = next;
  render();
}

function getWeekDates() {
  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date(state.weekStart);
    date.setDate(date.getDate() + index);
    return date;
  });
}

function startOfWeek(date) {
  const next = new Date(date);
  next.setHours(0, 0, 0, 0);
  const day = (next.getDay() + 6) % 7;
  next.setDate(next.getDate() - day);
  return next;
}

function dateKey(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatMonthDay(date) {
  return `${date.getMonth() + 1}/${date.getDate()}`;
}

function formatClock(date) {
  return `${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}

function formatTime(minutes) {
  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;
  return `${String(hours).padStart(2, "0")}:${String(mins).padStart(2, "0")}`;
}

function formatDuration(minutes) {
  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;
  if (!hours) return `${mins} 分`;
  if (!mins) return `${hours} 小時`;
  return `${hours} 小時 ${mins} 分`;
}

function formatElapsed(seconds) {
  const hours = Math.floor(seconds / 3600);
  const mins = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;
  return [hours, mins, secs].map((value) => String(value).padStart(2, "0")).join(":");
}

function timeToMinutes(value) {
  const [hours, minutes] = value.split(":").map(Number);
  return hours * 60 + minutes;
}

function minutesFromDate(date) {
  return date.getHours() * 60 + date.getMinutes();
}

function timerEntryRange(startedAt, endedAt) {
  let start = minutesFromDate(startedAt);
  let end = dateKey(startedAt) === dateKey(endedAt) ? minutesFromDate(endedAt) : 1440;
  const duration = Math.max(1, Math.ceil((endedAt.getTime() - startedAt.getTime()) / 60000));

  if (end <= start) end = start + 1;
  if (end > 1439) {
    end = 1439;
    start = Math.min(start, 1438);
  }

  return { start, end, duration };
}

function yToMinutes(y) {
  return Math.round((y / hourHeight()) * 60);
}

function minutesToY(minutes) {
  return (minutes / 60) * hourHeight();
}

function hourToY(hour) {
  return minutesToY(hour * 60);
}

function hourHeight() {
  return Number.parseFloat(getComputedStyle(document.documentElement).getPropertyValue("--hour-height"));
}

function snapMinutes(minutes) {
  return clamp(Math.round(minutes / 15) * 15, 0, 1440);
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
}

function persist() {
  localStorage.setItem("timeBlocks.entries", JSON.stringify(state.entries));
}

function loadEntries() {
  try {
    return (JSON.parse(localStorage.getItem("timeBlocks.entries")) || []).filter(isValidEntry);
  } catch {
    return [];
  }
}

function isValidEntry(entry) {
  return entry
    && typeof entry.date === "string"
    && Number.isFinite(entry.start)
    && Number.isFinite(entry.end)
    && entry.start >= 0
    && entry.end <= 1440
    && entry.end > entry.start;
}

function scrollToMorning() {
  scrollToTime(7 * 60);
}

function scrollToTime(minutes) {
  const scroller = document.querySelector(".calendar");
  scroller.scrollTop = Math.max(0, minutesToY(minutes) - 120);
}

function downloadFile(filename, content, type) {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
  showToast("已下載");
}

function csvCell(value) {
  return `"${String(value ?? "").replaceAll('"', '""')}"`;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

let toastTimer = null;
function showToast(message) {
  clearTimeout(toastTimer);
  els.toast.textContent = message;
  els.toast.classList.add("show");
  toastTimer = setTimeout(() => els.toast.classList.remove("show"), 1800);
}

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker
      .register("./service-worker.js")
      .then((registration) => registration.update())
      .catch(() => {});
  });
}
