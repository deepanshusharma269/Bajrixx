import { useEffect, useState } from 'react';
import { api, setSellerId } from './api';
import Buyer from './Buyer.jsx';
import Seller from './Seller.jsx';

export default function App() {
  const [tab, setTab] = useState('buyer');
  const [sellers, setSellers] = useState([]);
  const [sellerId, setSeller] = useState(() => localStorage.getItem('sellerId') || '');

  useEffect(() => {
    api.get('/sellers').then(setSellers).catch(() => {});
  }, []);

  const seller = sellers.find((s) => String(s.id) === sellerId);
  // Set synchronously so child effects send the right header on first fetch.
  setSellerId(seller ? seller.id : null);

  const pick = (id) => {
    setSeller(id);
    localStorage.setItem('sellerId', id);
  };

  return (
    <div className="app">
      <header className="topbar">
        <h1>BajriX</h1>
        <nav>
          <button className={tab === 'buyer' ? 'on' : ''} onClick={() => setTab('buyer')}>Buy</button>
          <button className={tab === 'seller' ? 'on' : ''} onClick={() => setTab('seller')}>Sell</button>
        </nav>
        {tab === 'seller' && (
          <label className="who">
            Acting as
            <select value={sellerId} onChange={(e) => pick(e.target.value)}>
              <option value="">Select seller…</option>
              {sellers.map((s) => (
                <option key={s.id} value={s.id}>{s.name} ({s.status})</option>
              ))}
            </select>
          </label>
        )}
      </header>
      <main>
        {tab === 'buyer' && <Buyer />}
        {tab === 'seller' &&
          (seller ? <Seller key={seller.id} seller={seller} /> : <p className="empty">Pick a seller above (mock login).</p>)}
      </main>
    </div>
  );
}
