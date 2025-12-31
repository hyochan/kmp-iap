import React from 'react';
import {IAPKIT_URL, TRACKING_URL} from '../constants';

interface IapKitLinkProps {
  children: React.ReactNode;
  href?: string;
  className?: string;
  style?: React.CSSProperties;
}

export default function IapKitLink({
  children,
  href,
  className,
  style,
}: IapKitLinkProps) {
  const handleClick = async () => {
    try {
      await fetch(TRACKING_URL, {
        method: 'POST',
        mode: 'no-cors',
      });
    } catch (error) {
      // Silently fail - don't block navigation
      console.error('Failed to track link click:', error);
    }
  };

  return (
    <a
      href={href || IAPKIT_URL}
      target="_blank"
      rel="noopener noreferrer"
      onClick={handleClick}
      className={className}
      style={style}
    >
      {children}
    </a>
  );
}
