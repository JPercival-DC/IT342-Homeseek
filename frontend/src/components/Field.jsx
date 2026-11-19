/** Labeled input with an inline error message that screen readers announce. */
export default function Field({ id, label, error, hint, ...inputProps }) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input id={id} name={id} aria-invalid={error ? 'true' : 'false'}
        aria-describedby={error ? `${id}-error` : hint ? `${id}-hint` : undefined} {...inputProps} />
      {hint && !error && <small id={`${id}-hint`} className="muted">{hint}</small>}
      {error && <small id={`${id}-error`} className="error" role="alert">{error}</small>}
    </div>
  );
}
