import ThemeToggle from './theme-toggle';

export const metadata = { title:'Страница не найдена | BublAPI Dent', description:'Запрошенная страница не найдена.', robots:{index:false,follow:false} };
export default function NotFound(){return <>
  <ThemeToggle floating />
  <main className="not-found-shell"><section className="card not-found-card"><div className="not-found-code">404</div><h1>Такой страницы нет</h1><p className="muted">Возможно, адрес изменился или был введён с ошибкой.</p><div className="actions" style={{justifyContent:'center',marginTop:24}}><a className="btn btn-primary" href="/">На главную</a><a className="btn btn-secondary" href="/doctors">Наши врачи</a></div></section></main>
</>}
