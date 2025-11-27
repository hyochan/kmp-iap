import React from 'react';
import useBaseUrl from '@docusaurus/useBaseUrl';

interface BannerLink {
  title: string;
  url: string;
}

const bannerLinks: BannerLink[] = [
  {
    title: 'Coding interview questions',
    url: 'https://www.greatfrontend.com/prepare/coding?fpr=hyo73',
  },
  {
    title: 'Quiz interview questions',
    url: 'https://www.greatfrontend.com/questions/formats/quiz?fpr=hyo73',
  },
  {
    title: 'Front End System design questions',
    url: 'https://www.greatfrontend.com/questions/formats/system-design?fpr=hyo73',
  },
  {
    title: 'Behavioral interview questions',
    url: 'https://www.greatfrontend.com/behavioral-interview-playbook?fpr=hyo73',
  },
  {
    title: 'Front End Interview Guidebook',
    url: 'https://www.greatfrontend.com/front-end-interview-playbook?fpr=hyo73',
  },
  {
    title: 'Front End System Design Guidebook',
    url: 'https://www.greatfrontend.com/front-end-system-design-playbook?fpr=hyo73',
  },
  {
    title: 'Study Plan',
    url: 'https://www.greatfrontend.com/interviews/study-plans?fpr=hyo73',
  },
  {
    title: 'Framework-specific practice questions',
    url: 'https://www.greatfrontend.com/questions?fpr=hyo73',
  },
  {
    title: 'JavaScript interview questions',
    url: 'https://www.greatfrontend.com/questions/js?fpr=hyo73',
  },
];

interface GreatFrontEndBannerProps {
  className?: string;
  style?: React.CSSProperties;
}

export default function GreatFrontEndBanner({
  className = 'greatfrontend-banner',
  style,
}: GreatFrontEndBannerProps) {
  // Select a random link for this page
  const randomLink = React.useMemo(
    () => bannerLinks[Math.floor(Math.random() * bannerLinks.length)],
    [],
  );

  const imageUrl = useBaseUrl('/img/greatfrontend-js.gif');

  return (
    <div className={className} style={style}>
      <a
        href={randomLink.url}
        target="_blank"
        rel="noopener noreferrer"
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
          alt="GreatFrontEnd - Front End Interview Prep"
          style={{
            maxWidth: '100%',
            height: 'auto',
            borderRadius: '8px',
          }}
        />
        <p
          style={{
            fontSize: '14px',
            color: '#0066cc',
            fontWeight: '500',
            cursor: 'pointer',
            textDecoration: 'underline',
          }}
        >
          {randomLink.title}
        </p>
      </a>
    </div>
  );
}
