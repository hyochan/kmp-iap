import clsx from "clsx";
import Link from "@docusaurus/Link";
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import Layout from "@theme/Layout";
import HomepageFeatures from "@site/src/components/HomepageFeatures";
import Heading from "@theme/Heading";

import styles from "./index.module.css";

function HomepageHeader() {
  const { siteConfig } = useDocusaurusContext();
  return (
    <header className="hero">
      <div className="container">
        <div className={styles.logoContainer}>
          <img
            src="img/logo.png"
            alt="kmp-iap logo"
            className={styles.logo}
          />
        </div>
        <Heading as="h1" className="hero__title">
          {siteConfig.title}
        </Heading>
        <p className="hero__subtitle">{siteConfig.tagline}</p>
        <div className={styles.buttons}>
          <Link
            className="button button--primary button--lg"
            to="/docs/getting-started/installation"
          >
            Get Started →
          </Link>
          <Link
            className="button button--secondary button--lg"
            to="/docs/intro"
          >
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
      icon: "🚀",
      title: "Quick Start",
      description: "Get up and running with kmp-iap in minutes",
      to: "/docs/getting-started/installation",
      gradient: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
    },
    {
      icon: "📚",
      title: "Guides",
      description: "Step-by-step tutorials and best practices",
      to: "/docs/guides/purchases",
      gradient: "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)",
    },
    {
      icon: "⚡",
      title: "API Reference",
      description: "Complete API documentation with examples",
      to: "/docs/api",
      gradient: "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)",
    },
    {
      icon: "💻",
      title: "Examples",
      description: "Production-ready code samples",
      to: "/docs/examples/basic-store",
      gradient: "linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)",
    },
  ];

  return (
    <section className="next-steps-section">
      <div className="container">
        <div className="next-steps-header">
          <h2 className="next-steps-title">Start Building Today</h2>
          <p className="next-steps-subtitle">
            Everything you need to implement in-app purchases in your Kotlin
            Multiplatform app
          </p>
        </div>
        <div className="next-steps-grid">
          {links.map((link, idx) => (
            <Link
              key={idx}
              className="next-step-card"
              to={link.to}
              style={
                { "--card-gradient": link.gradient } as React.CSSProperties
              }
            >
              <div className="next-step-icon-wrapper">
                <span className="next-step-icon">{link.icon}</span>
              </div>
              <h3 className="next-step-title">{link.title}</h3>
              <p className="next-step-description">{link.description}</p>
              <span className="next-step-arrow">→</span>
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
  const { siteConfig } = useDocusaurusContext();
  return (
    <Layout
      title={`${siteConfig.title} - Kotlin Multiplatform In-App Purchase Library`}
      description="A comprehensive Kotlin Multiplatform library for in-app purchases on Android and iOS platforms. Unified API, type-safe, modern architecture."
    >
      <HomepageHeader />
      <main>
        <HomepageFeatures />
        <CodeExample />
        <QuickLinks />
      </main>
    </Layout>
  );
}
