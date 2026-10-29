import React, { useState, useEffect } from 'react';
import { AppProviders } from './providers';
import { renderRoute } from './routes';
import AiAssistantWidget from '../components/common/AiAssistantWidget';

const App = () => {
  const [currentPath, setCurrentPath] = useState(window.location.pathname);

  useEffect(() => {
    const onLocationChange = () => {
      setCurrentPath(window.location.pathname);
    };
    window.addEventListener('popstate', onLocationChange);
    return () => window.removeEventListener('popstate', onLocationChange);
  }, []);

  return (
    <AppProviders>
      {renderRoute(currentPath)}
      <AiAssistantWidget />
    </AppProviders>
  );
};

export default App;
