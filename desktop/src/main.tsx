import React from 'react';
import ReactDOM from 'react-dom/client';
import { App } from './App';
import { DmsProvider } from './context/DmsContext';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <DmsProvider>
      <App />
    </DmsProvider>
  </React.StrictMode>
);
