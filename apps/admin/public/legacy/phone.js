const COUNTRY_OPTIONS = [
  { code: '7', label: '🇷🇺/🇰🇿 +7', localLength: 10 },
  { code: '7', label: '🇷🇺 +7', localLength: 10 },
  { code: '375', label: '🇧🇾 +375', localLength: 9 },
  { code: '380', label: '🇺🇦 +380', localLength: 9 },
  { code: '998', label: '🇺🇿 +998', localLength: 9 },
  { code: '996', label: '🇰🇬 +996', localLength: 9 },
  { code: '374', label: '🇦🇲 +374', localLength: 8 },
  { code: '995', label: '🇬🇪 +995', localLength: 9 },
  { code: '994', label: '🇦🇿 +994', localLength: 9 },
  { code: '373', label: '🇲🇩 +373', localLength: 8 },
  { code: '1', label: '🇺🇸 +1', localLength: 10 },
  { code: '44', label: '🇬🇧 +44', localLength: 10 }
];

function onlyDigits(value) {
  return String(value ?? '').replace(/\D/g, '');
}

function findCountryByNumber(digits) {
  const ordered = [...COUNTRY_OPTIONS].sort(
      (a, b) => b.code.length - a.code.length
  );

  return ordered.find(country =>
    digits.startsWith(country.code)
    && digits.length > country.code.length
  ) ?? COUNTRY_OPTIONS[0];
}

function formatRussian(local) {
  const digits = local.slice(0, 10);
  const a = digits.slice(0, 3);
  const b = digits.slice(3, 6);
  const c = digits.slice(6, 8);
  const d = digits.slice(8, 10);

  let result = '';
  if (a) result += `(${a}`;
  if (a.length === 3) result += ') ';
  if (b) result += b;
  if (c) result += `-${c}`;
  if (d) result += `-${d}`;
  return result;
}

function formatGeneric(local, maxLength) {
  const digits = local.slice(0, maxLength);
  const groups = [];
  let cursor = 0;

  while (cursor < digits.length) {
    const remaining = digits.length - cursor;
    const size = remaining > 4 ? 3 : remaining > 2 ? 2 : remaining;
    groups.push(digits.slice(cursor, cursor + size));
    cursor += size;
  }

  return groups.join(' ');
}

function updatePhoneState(input, countrySelect, localDigits) {
  const country = COUNTRY_OPTIONS.find(
      item => item.code === countrySelect.value
  ) ?? COUNTRY_OPTIONS[0];
  const normalizedLocal = onlyDigits(localDigits).slice(0, country.localLength);

  input.value = country.code === '7' || country.code === '1'
      ? formatRussian(normalizedLocal)
      : formatGeneric(normalizedLocal, country.localLength);
  input.dataset.phoneNormalized = normalizedLocal
      ? `${country.code}${normalizedLocal}`
      : '';
  input.dataset.phoneLocalDigits = normalizedLocal;
  input.setCustomValidity(
      input.required && normalizedLocal.length !== country.localLength
          ? `Введите ${country.localLength} цифр номера`
          : ''
  );
}

function enhancePhoneInput(input) {
  if (!(input instanceof HTMLInputElement) || input.dataset.phoneEnhanced) {
    return;
  }

  input.dataset.phoneEnhanced = 'true';
  input.inputMode = 'numeric';
  input.autocomplete = 'tel-national';
  input.placeholder = '(999) 123-45-67';

  const originalDigits = onlyDigits(input.value);
  const detectedCountry = originalDigits
      ? findCountryByNumber(originalDigits)
      : COUNTRY_OPTIONS[0];
  const initialLocal = originalDigits.startsWith(detectedCountry.code)
      ? originalDigits.slice(detectedCountry.code.length)
      : originalDigits;

  const wrapper = document.createElement('div');
  wrapper.className = 'phone-input-group';

  const countrySelect = document.createElement('select');
  countrySelect.className = 'phone-country-select';
  countrySelect.setAttribute('aria-label', 'Регион номера телефона');
  countrySelect.title = 'Регион номера телефона';

  COUNTRY_OPTIONS.forEach(country => {
    const option = document.createElement('option');
    option.value = country.code;
    option.textContent = country.label;
    option.selected = country.code === detectedCountry.code;
    countrySelect.append(option);
  });

  input.parentNode.insertBefore(wrapper, input);
  wrapper.append(countrySelect, input);

  const syncFromInput = () => {
    updatePhoneState(
        input,
        countrySelect,
        onlyDigits(input.value)
    );
  };

  input.addEventListener('input', syncFromInput);
  input.addEventListener('paste', () => {
    window.setTimeout(syncFromInput);
  });
  countrySelect.addEventListener('change', () => {
    updatePhoneState(
        input,
        countrySelect,
        input.dataset.phoneLocalDigits || onlyDigits(input.value)
    );
    input.dispatchEvent(new Event('change', { bubbles: true }));
  });

  updatePhoneState(input, countrySelect, initialLocal);
}

function enhanceAllPhoneInputs(root = document) {
  root.querySelectorAll?.('input[name="phone"]').forEach(enhancePhoneInput);
}

enhanceAllPhoneInputs();

new MutationObserver(mutations => {
  for (const mutation of mutations) {
    mutation.addedNodes.forEach(node => {
      if (node instanceof Element) {
        if (node.matches('input[name="phone"]')) {
          enhancePhoneInput(node);
        }
        enhanceAllPhoneInputs(node);
      }
    });
  }
}).observe(document.documentElement, {
  childList: true,
  subtree: true
});
