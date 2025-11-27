import React from 'react';
import useBaseUrl from '@docusaurus/useBaseUrl';

export default function GreatFrontEndTopFixed() {
  const imageUrl = useBaseUrl('/img/greatfrontend-js.gif');

  return (
    <div
      style={{
        marginBottom: 20,
        display: 'flex',
        justifyContent: 'center',
      }}
    >
      <a
        href="https://www.greatfrontend.com?fpr=hyo73"
        target="_blank"
        rel="noopener noreferrer"
        style={{
          display: 'block',
          textAlign: 'center',
          textDecoration: 'none',
        }}
      >
        <img
          src={imageUrl}
          alt="GreatFrontEnd - Front End Interview Prep"
          style={{
            maxWidth: '100%',
            height: 'auto',
            borderRadius: '8px',
          }}
        />
      </a>
    </div>
  );
}
