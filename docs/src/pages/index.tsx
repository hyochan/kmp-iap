import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import HomepageFeatures from '@site/src/components/HomepageFeatures';
import Heading from '@theme/Heading';

import styles from './index.module.css';

function HomepageHeader() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <header className="hero">
      <div className="container">
        <div className={styles.logoContainer}>
          <img src="/img/logo.png" alt="kmp-iap logo" className={styles.logo} />
        </div>
        <Heading as="h1" className="hero__title">
          {siteConfig.title}
        </Heading>
        <p className="hero__subtitle">{siteConfig.tagline}</p>
        <div className={styles.buttons}>
          <Link
            className="button button--primary button--lg"
            to="/docs/getting-started/installation">
            Get Started →
          </Link>
          <Link
            className="button button--secondary button--lg"
            to="/docs/intro">
            Documentation
          </Link>
        </div>
      </div>
    </header>
  );
}

function QuickLinks() {
  const links = [
    {
      icon: '🚀',
      title: 'Getting Started',
      description: 'Learn how to install and configure kmp-iap in your project.',
      to: '/docs/getting-started/installation',
    },
    {
      icon: '📖',
      title: 'Guides',
      description: 'Follow step-by-step guides for implementing purchases and subscriptions.',
      to: '/docs/guides/basic-setup',
    },
    {
      icon: '🔧',
      title: 'API Reference',
      description: 'Comprehensive API documentation with examples and type definitions.',
      to: '/docs/api',
    },
    {
      icon: '💡',
      title: 'Examples',
      description: 'Real-world examples and implementation patterns.',
      to: '/docs/examples/basic-store',
    },
  ];

  return (
    <section className="homepage-section">
      <div className="container">
        <h2 className="section__title">📚 What's Next?</h2>
        <div className="quick-links">
          {links.map((link, idx) => (
            <Link
              key={idx}
              className="quick-link-card"
              to={link.to}>
              <span className="quick-link-icon">{link.icon}</span>
              <h3 className="quick-link-title">{link.title}</h3>
              <p>{link.description}</p>
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
}

function CodeExample() {
  return (
    <section className="homepage-section">
      <div className="container">
        <h2 className="section__title">Simple to Use</h2>
        <div className={styles.codeExample}>
          <pre>
            <code className="language-kotlin">{`// Initialize
KmpIAP.initConnection()

// Load products
val products = KmpIAP.requestProducts(
    ProductRequest(
        skus = listOf("product_id"),
        type = ProductType.INAPP
    )
)

// Make purchase
KmpIAP.requestPurchase(
    UnifiedPurchaseRequest(
        sku = "product_id",
        quantity = 1
    )
)

// Listen to updates
KmpIAP.purchaseUpdatedListener.collect { purchase ->
    // Handle purchase
}`}</code>
          </pre>
        </div>
      </div>
    </section>
  );
}

export default function Home(): JSX.Element {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      title={`${siteConfig.title} - Kotlin Multiplatform In-App Purchase Library`}
      description="A comprehensive Kotlin Multiplatform library for in-app purchases on Android and iOS platforms. Unified API, type-safe, modern architecture.">
      <HomepageHeader />
      <main>
        <HomepageFeatures />
        <CodeExample />
        <QuickLinks />
      </main>
    </Layout>
  );
}