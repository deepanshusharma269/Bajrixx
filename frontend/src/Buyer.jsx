import { useEffect, useState } from 'react';
import { api, money } from './api';

const PAGE_SIZE = 12;

export default function Buyer() {
  const [q, setQ] = useState('');
  const [debouncedQ, setDebouncedQ] = useState('');
  const [category, setCategory] = useState('');
  const [sort, setSort] = useState('name');
  const [page, setPage] = useState(0);
  const [categories, setCategories] = useState([]);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    api.get('/categories').then(setCategories).catch(() => {});
  }, []);

  useEffect(() => {
    const t = setTimeout(() => {
      setDebouncedQ(q);
      setPage(0);
    }, 300);
    return () => clearTimeout(t);
  }, [q]);

  useEffect(() => {
    let stale = false;
    setLoading(true);
    setError('');
    const params = new URLSearchParams({ q: debouncedQ, category, sort, page, size: PAGE_SIZE });
    api.get('/products?' + params)
      .then((d) => !stale && setData(d))
      .catch((e) => !stale && setError(e.message))
      .finally(() => !stale && setLoading(false));
    return () => {
      stale = true;
    };
  }, [debouncedQ, category, sort, page]);

  const pages = data ? Math.max(1, Math.ceil(data.total / PAGE_SIZE)) : 1;

  return (
    <section>
      <div className="filters">
        <input placeholder="Search cement, TMT, bricks…" value={q} onChange={(e) => setQ(e.target.value)} />
        <select value={category} onChange={(e) => { setCategory(e.target.value); setPage(0); }}>
          <option value="">All categories</option>
          {categories.map((c) => <option key={c}>{c}</option>)}
        </select>
        <select value={sort} onChange={(e) => { setSort(e.target.value); setPage(0); }}>
          <option value="name">Name</option>
          <option value="price_asc">Price: low to high</option>
          <option value="price_desc">Price: high to low</option>
        </select>
      </div>

      {error && <p className="error">{error}</p>}
      {loading && !data && <p className="empty">Loading…</p>}
      {data && data.items.length === 0 && <p className="empty">No products match your search.</p>}

      <div className={'grid' + (loading ? ' dim' : '')}>
        {data?.items.map((p) => (
          <button key={p.id} className="card" onClick={() => setSelected(p.id)}>
            <span className="tag">{p.category}</span>
            <h3>{p.name}</h3>
            <p className="muted">{p.brand}</p>
            <p className="price">from {money(p.minPrice)} <small>/ {p.unit}</small></p>
            <p className="muted">{p.offerCount} seller{p.offerCount > 1 ? 's' : ''}</p>
          </button>
        ))}
      </div>

      {data && data.total > PAGE_SIZE && (
        <div className="pager">
          <button disabled={page === 0} onClick={() => setPage(page - 1)}>Prev</button>
          <span>Page {page + 1} of {pages}</span>
          <button disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>Next</button>
        </div>
      )}

      {selected && <ProductModal id={selected} onClose={() => setSelected(null)} />}
    </section>
  );
}

function ProductModal({ id, onClose }) {
  const [p, setP] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/products/' + id).then(setP).catch((e) => setError(e.message));
  }, [id]);

  const bestId = p?.offers.find((o) => o.inStock)?.listingId;

  return (
    <div className="overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <button className="close" onClick={onClose}>×</button>
        {error && <p className="error">{error}</p>}
        {!p && !error && <p>Loading…</p>}
        {p && (
          <>
            <span className="tag">{p.category}</span>
            <h2>{p.name}</h2>
            <p className="muted">{p.brand} · priced per {p.unit}</p>
            {p.offers.length === 0 ? (
              <p className="empty">No sellers currently offer this product.</p>
            ) : (
              <table>
                <thead>
                  <tr><th>Seller</th><th>City</th><th>Price</th><th>Stock</th><th>Min order</th></tr>
                </thead>
                <tbody>
                  {p.offers.map((o) => (
                    <tr key={o.listingId} className={o.inStock ? '' : 'out'}>
                      <td>{o.sellerName} {o.listingId === bestId && <span className="best">Best price</span>}</td>
                      <td>{o.city}</td>
                      <td>{money(o.price)}</td>
                      <td>{o.inStock ? o.stock : 'Out of stock'}</td>
                      <td>{o.minOrderQty}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </>
        )}
      </div>
    </div>
  );
}
