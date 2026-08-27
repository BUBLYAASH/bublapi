export function onlyDigits(value) {
  return String(value ?? '').replace(/\D/g, '');
}

function formatGroups(value, sizes, separators = ' ') {
  const digits = onlyDigits(value);
  let cursor = 0;
  let result = '';

  sizes.forEach((size, index) => {
    if (cursor >= digits.length) return;

    const group = digits.slice(cursor, cursor + size);
    if (result) {
      result += Array.isArray(separators)
          ? separators[index - 1] ?? ' '
          : separators;
    }
    result += group;
    cursor += group.length;
  });

  return result;
}

function formatWithRules(digits, rules, fallbackGroups, fallbackSeparators = ' ') {
  const rule = rules.find(item => item.leadingDigits.test(digits));
  return formatGroups(
      digits,
      rule?.groups ?? fallbackGroups,
      rule?.separators ?? fallbackSeparators
  );
}

function formatParenthesized(digits, tailGroups) {
  const areaCode = digits.slice(0, 3);
  const tail = digits.slice(3);
  let result = areaCode ? `(${areaCode}` : '';

  if (areaCode.length === 3) result += ')';
  if (tail) result += ` ${formatGroups(tail, tailGroups, '-')}`;

  return result;
}

// Kazakhstan shares +7 with Russia, but its published grouping is selected
// from the national prefix. These are the two exceptional fixed-line shapes
// from libphonenumber metadata; ordinary +7 Kazakhstan numbers use 3-3-4.
const KZ_FOUR_DIGIT_AREA = /^(?:7(?:1(?:[0-356]2|4[29]|7|8[27])|2(?:13[03-69]|62[013-9]))|72[1-57-9]2)/;
const KZ_FIVE_DIGIT_AREA = /^(?:7(?:1(?:0(?:[356]|4[023])|[18]|2(?:3[013-9]|5)|3[45]|43[013-79]|5(?:3[1-8]|4[1-7]|5)|6(?:3[0-35-9]|[4-6]))|2(?:1(?:3[178]|[45])|[24-689]|3[35]|7[457]))|7(?:14|23)4[0-8]|71(?:33|45)[1-79])/;

function formatPlusSeven(digits) {
  if (!digits.startsWith('7')) {
    return formatParenthesized(digits, [3, 2, 2]);
  }

  if (KZ_FOUR_DIGIT_AREA.test(digits)) {
    return formatGroups(digits, [4, 2, 2, 2]);
  }

  if (KZ_FIVE_DIGIT_AREA.test(digits)) {
    return formatGroups(digits, [5, 1, 2, 2]);
  }

  return formatGroups(digits, [3, 3, 4]);
}

const FORMATTERS = {
  '7': formatPlusSeven,
  '375': digits => formatWithRules(digits, [
    {
      leadingDigits: /^(?:1(?:5[169]|6(?:3[1-3]|4|5[125])|7(?:1[3-9]|7[0-24-6]|9[2-7]))|2(?:1[35]|2[34]|3[3-5]))/,
      groups: [4, 2, 3],
      separators: [' ', '-']
    },
    {
      leadingDigits: /^(?:1(?:[56]|7[467])|2[1-3])/,
      groups: [3, 2, 2, 2],
      separators: [' ', '-', '-']
    }
  ], [2, 3, 2, 2], [' ', '-', '-']),
  '380': digits => formatWithRules(digits, [
    {
      leadingDigits: /^(?:6[12][29]|(?:35|4[1378]|5[12457]|6[49])2|(?:56|65)[24]|(?:3[1-46-8]|46)2[013-9])/,
      groups: [3, 3, 3]
    },
    {
      leadingDigits: /^(?:3[1-8]|4(?:[1367]|[45][6-9]|8[4-6])|5(?:[1-5]|6(?:[015689]|3[02389])|7[4-6])|6(?:[12][3-7]|[459]))/,
      groups: [4, 5]
    },
    { leadingDigits: /^(?:[3-7]|89|9[1-9])/, groups: [2, 3, 4] }
  ], [3, 3, 3]),
  '998': digits => formatGroups(digits, [2, 3, 2, 2]),
  '996': digits => formatWithRules(digits, [
    { leadingDigits: /^3(?:1[346]|[24-79])/, groups: [4, 5] },
    { leadingDigits: /^(?:[235-79]|88)/, groups: [3, 3, 3] },
    { leadingDigits: /^8/, groups: [3, 3, 1, 2] }
  ], [3, 3, 3]),
  '374': digits => formatWithRules(digits, [
    { leadingDigits: /^[89]0/, groups: [3, 2, 3] },
    { leadingDigits: /^(?:2|3[12])/, groups: [3, 5] }
  ], [2, 6]),
  '995': digits => formatWithRules(digits, [
    { leadingDigits: /^70/, groups: [3, 3, 3] },
    { leadingDigits: /^32/, groups: [2, 3, 2, 2] }
  ], [3, 2, 2, 2]),
  '994': digits => formatWithRules(digits, [
    { leadingDigits: /^90/, groups: [3, 2, 2, 2] }
  ], [2, 3, 2, 2]),
  '373': digits => formatWithRules(digits, [
    { leadingDigits: /^[89]/, groups: [3, 5] },
    { leadingDigits: /^(?:22|3)/, groups: [2, 3, 3] }
  ], [3, 2, 3]),
  '1': digits => formatParenthesized(digits, [3, 4]),
  '44': digits => formatWithRules(digits, [
    {
      leadingDigits: /^1(?:3873|5(?:242|39[4-6])|(?:697|768)[347]|9467)/,
      groups: [5, 5]
    },
    { leadingDigits: /^1(?:[2-69][02-9]|[78])/, groups: [4, 6] },
    {
      leadingDigits: /^(?:[25]|7(?:0|6(?:[03-9]|2[356])))/,
      groups: [2, 4, 4]
    },
    { leadingDigits: /^7/, groups: [4, 6] }
  ], [3, 3, 4])
};

export function formatLocalPhone(countryCode, localDigits) {
  const digits = onlyDigits(localDigits);
  const formatter = FORMATTERS[String(countryCode)];
  return formatter ? formatter(digits) : formatGroups(digits, [3, 3, 4]);
}

export function normalizeLocalPhoneInput(country, value) {
  const digits = onlyDigits(value);
  const localLength = Number(country.localLength);

  if (digits.length > localLength && digits.startsWith(country.code)) {
    return digits.slice(country.code.length);
  }

  for (const prefix of country.nationalPrefixes ?? []) {
    if (!digits.startsWith(prefix)) continue;
    const withoutPrefix = digits.slice(prefix.length);
    if (withoutPrefix.length > 0 && withoutPrefix.length <= localLength) {
      return withoutPrefix;
    }
  }

  return digits;
}
