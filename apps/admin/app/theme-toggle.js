import { themeToggleMarkup } from '../lib/theme-toggle';

export default function ThemeToggle({ id = 'themeToggle', floating = false }) {
  const markup = themeToggleMarkup
    .replace('id="themeToggle"', `id="${id}"`)
    .replace('class="theme-toggle"', `class="theme-toggle${floating ? ' theme-toggle-floating' : ''}"`);

  return <span dangerouslySetInnerHTML={{ __html: markup }} />;
}
