const FLASH_STORAGE_KEY = "sivemor.web.flash";

export function setFlashMessage(message) {
  if (typeof window === "undefined") {
    return;
  }

  if (!message) {
    window.sessionStorage.removeItem(FLASH_STORAGE_KEY);
    return;
  }

  window.sessionStorage.setItem(FLASH_STORAGE_KEY, message);
}

export function consumeFlashMessage() {
  if (typeof window === "undefined") {
    return null;
  }

  const message = window.sessionStorage.getItem(FLASH_STORAGE_KEY);
  if (!message) {
    return null;
  }

  window.sessionStorage.removeItem(FLASH_STORAGE_KEY);
  return message;
}
