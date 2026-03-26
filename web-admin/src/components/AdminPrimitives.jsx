import { brandTokens } from "../lib/brand";

export function cx(...values) {
  return values.filter(Boolean).join(" ");
}

export function PagePanel({ children, className = "" }) {
  return <section className={cx("app-panel", className)}>{children}</section>;
}

export function PageTitleBar({ title, subtitle, search, actions }) {
  return (
    <div className="panel-header">
      <div>
        <h1 className="text-[1.7rem] font-bold leading-tight text-[var(--title)]">{title}</h1>
        {subtitle ? (
          <p className="mt-1 max-w-3xl text-sm text-[rgba(30,53,47,0.84)]">{subtitle}</p>
        ) : null}
      </div>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        {search}
        {actions}
      </div>
    </div>
  );
}

export function SearchField({ value, onChange, placeholder = "Search..." }) {
  return (
    <div className="relative min-w-[220px] max-sm:w-full">
      <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[var(--shell-text)]/70">
        <SearchIcon className="h-4 w-4" />
      </span>
      <input
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        className="field-base pl-10"
      />
    </div>
  );
}

export function PrimaryActionButton({ className = "", children, ...props }) {
  return (
    <button className={cx("btn-primary gap-2", className)} {...props}>
      {children}
    </button>
  );
}

export function SecondaryActionButton({ className = "", children, ...props }) {
  return (
    <button className={cx("btn-secondary gap-2", className)} {...props}>
      {children}
    </button>
  );
}

export function StatusChip({ label, tone = "neutral" }) {
  const toneMap = {
    neutral: { background: "var(--neutral-bg)", color: "var(--shell-text)" },
    success: { background: "var(--success-bg)", color: "var(--success-text)" },
    warning: { background: "var(--warning-bg)", color: "var(--warning-text)" },
    danger: { background: "#f3ddd7", color: "var(--danger)" }
  };

  return (
    <span
      className="inline-flex min-h-6 items-center rounded-md px-2.5 text-xs font-bold"
      style={toneMap[tone]}
    >
      {label}
    </span>
  );
}

export function EmptyState({ title, description }) {
  return (
    <div className="flex min-h-[220px] flex-col items-center justify-center px-6 py-10 text-center">
      <h3 className="text-lg font-bold text-[var(--title)]">{title}</h3>
      <p className="mt-2 max-w-md text-sm text-[var(--shell-text)]/85">{description}</p>
    </div>
  );
}

export function Modal({ open, title, children, footer, onClose }) {
  if (!open) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#183428]/35 p-4">
      <div className="w-full max-w-4xl overflow-hidden rounded-2xl border border-[var(--border)] bg-[var(--page)] shadow-[var(--shadow)]">
        <div className="flex items-center justify-between border-b border-[var(--border-strong)] bg-[#93b7a9] px-5 py-4">
          <h2 className="text-lg font-bold text-[var(--title)]">{title}</h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded-md p-1 text-[var(--title)] transition hover:bg-white/30"
            aria-label="Cerrar"
          >
            <CloseIcon className="h-5 w-5" />
          </button>
        </div>
        <div className="max-h-[70vh] overflow-y-auto px-5 py-5">{children}</div>
        <div className="flex flex-col-reverse gap-3 border-t border-[var(--border)] bg-[#f6faef] px-5 py-4 sm:flex-row sm:justify-end">
          {footer}
        </div>
      </div>
    </div>
  );
}

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = "Eliminar",
  cancelLabel = "Cancelar",
  onConfirm,
  onCancel,
  danger = false
}) {
  return (
    <Modal
      open={open}
      title={title}
      onClose={onCancel}
      footer={
        <>
          <SecondaryActionButton type="button" onClick={onCancel}>
            {cancelLabel}
          </SecondaryActionButton>
          <button
            type="button"
            onClick={onConfirm}
            className={danger ? "btn-danger" : "btn-primary"}
          >
            {confirmLabel}
          </button>
        </>
      }
    >
      <p className="text-sm leading-6 text-[var(--shell-text)]">{description}</p>
    </Modal>
  );
}

export function FieldError({ message }) {
  if (!message) {
    return null;
  }

  return <p className="mt-1 text-xs font-semibold text-[var(--danger)]">{message}</p>;
}

export function AlertMessage({ message }) {
  if (!message) {
    return null;
  }

  return (
    <div className="rounded-xl border border-[#d9b5ae] bg-[#fff3f0] px-4 py-3 text-sm font-medium text-[var(--danger)]">
      {message}
    </div>
  );
}

export function LinkText({ children }) {
  return <button className="text-left font-bold text-[var(--shell-dark)] underline">{children}</button>;
}

export function PlusIcon({ className = "h-4 w-4" }) {
  return (
    <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8" className={className}>
      <path d="M10 4v12M4 10h12" strokeLinecap="round" />
    </svg>
  );
}

export function EditIcon({ className = "h-4 w-4" }) {
  return (
    <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" className={className}>
      <path d="m13.75 3.75 2.5 2.5M4 16l3.1-.6L15.5 7a1.77 1.77 0 0 0 0-2.5l0-.01a1.77 1.77 0 0 0-2.5 0L4.6 12.9 4 16Z" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export function TrashIcon({ className = "h-4 w-4" }) {
  return (
    <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" className={className}>
      <path d="M4.5 6.25h11M8 9.25v4.5M12 9.25v4.5M6.25 6.25l.5 9.25h6.5l.5-9.25M7.5 6.25v-1a1 1 0 0 1 1-1h3a1 1 0 0 1 1 1v1" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export function MenuIcon({ className = "h-5 w-5" }) {
  return (
    <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8" className={className}>
      <path d="M3.5 5.5h13M3.5 10h13M3.5 14.5h13" strokeLinecap="round" />
    </svg>
  );
}

export function ChevronIcon({ className = "h-4 w-4" }) {
  return (
    <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8" className={className}>
      <path d="m7 4.5 5.5 5.5L7 15.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export function LogoutIcon({ className = "h-4 w-4" }) {
  return (
    <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" className={className}>
      <path d="M7 4.75H5.5A1.75 1.75 0 0 0 3.75 6.5v7A1.75 1.75 0 0 0 5.5 15.25H7M11.25 6.75 14.5 10l-3.25 3.25M8.25 10h6.25" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export function SearchIcon({ className = "h-4 w-4" }) {
  return (
    <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8" className={className}>
      <circle cx="9" cy="9" r="5.2" />
      <path d="m13 13 3.5 3.5" strokeLinecap="round" />
    </svg>
  );
}

export function CloseIcon({ className = "h-4 w-4" }) {
  return (
    <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8" className={className}>
      <path d="m5 5 10 10M15 5 5 15" strokeLinecap="round" />
    </svg>
  );
}

export function PageBadge({ children }) {
  return (
    <span
      className="inline-flex items-center rounded-md px-2.5 py-1 text-xs font-bold"
      style={{
        background: brandTokens.colors.panelAlt,
        color: brandTokens.colors.shellText
      }}
    >
      {children}
    </span>
  );
}
