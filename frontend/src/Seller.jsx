import { useCallback, useEffect, useState } from 'react';
import { api, money } from './api';

export default function Seller({ seller }) {
  const [rows, setRows] = useState(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const canEdit = seller.status === 'APPROVED';

  const load = useCallback(
    () => api.get('/seller/listings').then(setRows).catch((e) => setError(e.message)),
    []
  );
  useEffect(() => {
    load();
  }, [load]);

  const ok = (msg) => {
    setError('');
    setNotice(msg);
    load();
  };
  const fail = (e) => {
    setNotice('');
    setError(e.message);
    if (e.status === 409) load(); // stale version: show the latest values
  };

  return (
    <section>
      <h2>{seller.name} <small className={'status ' + seller.status}>{seller.status}</small></h2>
      {!canEdit && (
        <p className="error">
          Your account is {seller.status.toLowerCase()}. You can view your listings but not change them.
        </p>
      )}
      {error && <p className="error">{error}</p>}
      {notice && <p className="ok">{notice}</p>}

      {canEdit && <AddListing onAdded={() => ok('Listing added')} onError={fail} />}

      <h3>My listings</h3>
      {!rows && <p className="empty">Loading…</p>}
      {rows && rows.length === 0 && <p className="empty">You are not selling anything yet.</p>}
      {rows && rows.length > 0 && (
        <table>
          <thead>
            <tr><th>Product</th><th>Price (₹)</th><th>Stock</th><th>Min order</th><th></th></tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <Row key={r.id + '-' + r.version} r={r} canEdit={canEdit} onSaved={ok} onError={fail} />
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

function Row({ r, canEdit, onSaved, onError }) {
  const [price, setPrice] = useState(r.price);
  const [stock, setStock] = useState(r.stock);
  const [moq, setMoq] = useState(r.minOrderQty);
  const dirty = Number(price) !== Number(r.price) || Number(stock) !== r.stock || Number(moq) !== r.minOrderQty;

  const save = () =>
    api
      .put('/seller/listings/' + r.id, {
        price: Number(price),
        stock: Number(stock),
        minOrderQty: Number(moq),
        version: r.version,
      })
      .then(() => onSaved('Saved ' + r.productName))
      .catch(onError);

  const stop = () => {
    if (!window.confirm(`Stop selling "${r.productName}"?`)) return;
    api.del('/seller/listings/' + r.id).then(() => onSaved('Stopped selling ' + r.productName)).catch(onError);
  };

  return (
    <tr>
      <td>{r.productName} <small className="muted">/ {r.unit}</small></td>
      <td><input type="number" min="0.01" step="0.01" value={price} disabled={!canEdit} onChange={(e) => setPrice(e.target.value)} /></td>
      <td><input type="number" min="0" value={stock} disabled={!canEdit} onChange={(e) => setStock(e.target.value)} /></td>
      <td><input type="number" min="1" value={moq} disabled={!canEdit} onChange={(e) => setMoq(e.target.value)} /></td>
      <td className="actions">
        {canEdit && (
          <>
            <button className="primary" disabled={!dirty} onClick={save}>Save</button>
            <button className="danger" onClick={stop}>Stop selling</button>
          </>
        )}
      </td>
    </tr>
  );
}

function AddListing({ onAdded, onError }) {
  const [q, setQ] = useState('');
  const [options, setOptions] = useState([]);
  const [productId, setProductId] = useState('');
  const [price, setPrice] = useState('');
  const [stock, setStock] = useState('');
  const [moq, setMoq] = useState('1');

  useEffect(() => {
    const t = setTimeout(() => {
      api.get('/seller/catalog?q=' + encodeURIComponent(q)).then(setOptions).catch(() => {});
    }, 200);
    return () => clearTimeout(t);
  }, [q]);

  const submit = (e) => {
    e.preventDefault();
    api
      .post('/seller/listings', {
        productId: Number(productId),
        price: Number(price),
        stock: Number(stock),
        minOrderQty: Number(moq),
      })
      .then(() => {
        setProductId('');
        setPrice('');
        setStock('');
        setMoq('1');
        onAdded();
      })
      .catch(onError);
  };

  return (
    <form className="add" onSubmit={submit}>
      <h3>Add a product to sell</h3>
      <div className="row">
        <input placeholder="Filter catalogue…" value={q} onChange={(e) => setQ(e.target.value)} />
        <select required value={productId} onChange={(e) => setProductId(e.target.value)}>
          <option value="">Select product…</option>
          {options.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
        </select>
        <input required type="number" min="0.01" step="0.01" placeholder="Price ₹" value={price} onChange={(e) => setPrice(e.target.value)} />
        <input required type="number" min="0" placeholder="Stock" value={stock} onChange={(e) => setStock(e.target.value)} />
        <input required type="number" min="1" placeholder="Min order" value={moq} onChange={(e) => setMoq(e.target.value)} />
        <button className="primary" type="submit">Add</button>
      </div>
    </form>
  );
}
