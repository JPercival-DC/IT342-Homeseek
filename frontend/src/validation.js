// Client-side copies of the server's rules (Increments 1 and 2). They only save a round trip; the server decides.
export const rules = {
  fullName: (v) => (v.trim().length < 2 || v.trim().length > 100 ? 'Full name must be 2 to 100 characters' : ''),
  email: (v) => (/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(v.trim()) ? '' : 'Enter a valid email address'),
  phone: (v) => (/^\+?[0-9][0-9 -]{6,18}[0-9]$/.test(v.trim()) ? '' : 'Enter a valid phone number (8 to 20 digits)'),
  password: (v) => {
    if (v.length < 8 || v.length > 72) return 'Password must be 8 to 72 characters';
    if (!/^[\x20-\x7E]*$/.test(v)) return 'Password may only use standard keyboard characters';
    if (!/[A-Za-z]/.test(v) || !/\d/.test(v)) return 'Password must contain at least one letter and one number';
    return '';
  },
};

/** Runs the named rules over the form values and returns only the failures: { field: message }. */
export function check(values, fields) {
  const errors = {};
  for (const f of fields) {
    const message = rules[f](values[f]);
    if (message) errors[f] = message;
  }
  return errors;
}
