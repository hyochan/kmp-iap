import React from 'react';
import useBaseUrl from '@docusaurus/useBaseUrl';
import {IAPKIT_URL, TRACKING_URL} from '../constants';

interface IapKitBannerProps {
  className?: string;
  style?: React.CSSProperties;
}

export default function IapKitBanner({
  className = 'iapkit-banner',
  style,
}: IapKitBannerProps) {
  const imageUrl = useBaseUrl('/img/iapkit-banner.gif');

  const handleClick = async () => {
    try {
      await fetch(TRACKING_URL, {
        method: 'POST',
        mode: 'no-cors',
      });
    } catch (error) {
      // Silently fail - don't block navigation
      console.error('Failed to track banner click:', error);
    }
  };

  return (
    <div className={className} style={style}>
      <a
        href={IAPKIT_URL}
        target="_blank"
        rel="noopener noreferrer"
        onClick={handleClick}
        style={{
          display: 'block',
          textAlign: 'center',
          marginBottom: '20px',
          textDecoration: 'none',
          cursor: 'pointer',
        }}
      >
        <img
          src={imageUrl}
          alt="IAPKit - In-App Purchase Made Simple"
          style={{
            height: 'auto',
            borderRadius: '8px',
            objectFit: 'contain',
          }}
        />
      </a>
    </div>
  );
}
