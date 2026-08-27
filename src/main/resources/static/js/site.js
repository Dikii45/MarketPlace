document.addEventListener("click", (event) => {
  document.querySelectorAll("details.account-menu[open]").forEach((menu) => {
    if (!menu.contains(event.target)) menu.removeAttribute("open");
  });
});

document.addEventListener("keydown", (event) => {
  if (event.key !== "Escape") return;
  document.querySelectorAll("details.account-menu[open]").forEach((menu) => {
    menu.removeAttribute("open");
  });
});

document.addEventListener("click", (event) => {
  const thumb = event.target.closest("[data-gallery-thumb]");
  if (!thumb) return;
  const main = document.getElementById("galleryMain");
  if (!main) return;
  main.src = thumb.src;
  thumb.parentElement.querySelectorAll("[data-gallery-thumb]").forEach((el) => {
    el.classList.toggle("is-active", el === thumb);
  });
});

document.addEventListener("submit", (event) => {
  const message = event.target.getAttribute("data-confirm");
  if (message && !confirm(message)) event.preventDefault();
});

document.querySelectorAll("input[data-match]").forEach((confirmInput) => {
  const passwordInput = document.querySelector(confirmInput.getAttribute("data-match"));
  const form = confirmInput.closest("form");
  if (!passwordInput || !form) return;

  const checkMatch = () => {
    confirmInput.setCustomValidity(
      confirmInput.value && confirmInput.value !== passwordInput.value ? "Пароли не совпадают" : ""
    );
  };
  passwordInput.addEventListener("input", checkMatch);
  confirmInput.addEventListener("input", checkMatch);
  form.addEventListener("submit", (event) => {
    checkMatch();
    if (!confirmInput.reportValidity()) event.preventDefault();
  });
});

function syncFileInput(input, files) {
  const dt = new DataTransfer();
  files.forEach((file) => dt.items.add(file));
  input.files = dt.files;
}

function renderFilePreview(input) {
  const preview = document.getElementById(input.getAttribute("data-preview"));
  if (!preview) return;
  preview.innerHTML = "";
  input._selectedFiles.forEach((file, index) => {
    const item = document.createElement("div");
    item.className = "file-preview__item";

    const img = document.createElement("img");
    img.src = URL.createObjectURL(file);
    img.alt = file.name;
    img.onload = () => URL.revokeObjectURL(img.src);

    const remove = document.createElement("button");
    remove.type = "button";
    remove.className = "file-preview__remove";
    remove.textContent = "×";
    remove.setAttribute("aria-label", "Убрать фото");
    remove.addEventListener("click", () => {
      input._selectedFiles.splice(index, 1);
      syncFileInput(input, input._selectedFiles);
      renderFilePreview(input);
    });

    item.append(img, remove);
    preview.appendChild(item);
  });
}

document.addEventListener("change", (event) => {
  const avatarInput = event.target.closest("input[type=file][data-avatar-preview]");
  if (avatarInput) {
    const file = avatarInput.files[0];
    if (!file) return;
    const preview = document.getElementById(avatarInput.getAttribute("data-avatar-preview"));
    if (!preview) return;
    const img = document.createElement("img");
    img.alt = "";
    img.src = URL.createObjectURL(file);
    img.onload = () => URL.revokeObjectURL(img.src);
    preview.replaceChildren(img);
  }
});

document.addEventListener("change", (event) => {
  const input = event.target.closest("input[type=file][data-preview]");
  if (!input) return;

  const max = Number(input.getAttribute("data-max-files")) || Infinity;
  const previous = input._selectedFiles || [];
  const incoming = Array.from(input.files);
  const merged = previous.concat(incoming).slice(0, max);
  const exceeded = previous.length + incoming.length > max;

  input._selectedFiles = merged;
  syncFileInput(input, merged);
  renderFilePreview(input);

  if (exceeded) {
    input.setCustomValidity(`Можно выбрать не больше ${max} фото`);
    input.reportValidity();
  } else {
    input.setCustomValidity("");
  }
});

// ---------- чат: отправка сообщений и опрос новых раз в несколько секунд ----------
(function () {
  const container = document.getElementById("chatMessages");
  const form = document.getElementById("chatForm");
  if (!container || !form) return;

  const chatUrl = container.getAttribute("data-chat-url");
  let lastId = Number(container.getAttribute("data-last-id")) || 0;

  function scrollToBottom() {
    container.scrollTop = container.scrollHeight;
  }
  scrollToBottom();

  function appendHtml(html) {
    if (!html) return;
    const empty = container.querySelector(".empty-state");
    if (empty) empty.remove();
    container.insertAdjacentHTML("beforeend", html);
    const bubbles = container.querySelectorAll("[data-id]");
    if (bubbles.length) {
      lastId = Number(bubbles[bubbles.length - 1].getAttribute("data-id")) || lastId;
    }
    scrollToBottom();
  }

  async function poll() {
    try {
      const res = await fetch(`${chatUrl}/poll?afterId=${lastId}`);
      if (!res.ok) return;
      appendHtml(await res.text());
    } catch (e) {
      // сеть недоступна — попробуем на следующем тике
    }
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const input = form.querySelector("input[name=text]");
    const csrf = form.querySelector("input[name=_csrf]").value;
    const text = input.value.trim();
    if (!text) return;
    input.value = "";
    try {
      const res = await fetch(`${chatUrl}/send`, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: `text=${encodeURIComponent(text)}&_csrf=${encodeURIComponent(csrf)}`,
      });
      if (res.ok) appendHtml(await res.text());
    } catch (e) {
      input.value = text;
    }
  });

  setInterval(poll, 4000);
})();
