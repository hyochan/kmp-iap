import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  docsSidebar: [
    'intro',
    {
      type: 'category',
      label: 'Getting Started',
      collapsed: false,
      items: [
        'getting-started/installation',
        'getting-started/ios-setup',
        'getting-started/android-setup',
      ],
    },
    {
      type: 'category',
      label: 'Guides',
      items: [
        'guides/purchases',
        'guides/lifecycle',
        'guides/offer-code-redemption',
        'guides/troubleshooting',
        'guides/faq',
      ],
    },
    {
      type: 'category',
      label: 'API Reference',
      link: {
        type: 'doc',
        id: 'api/index',
      },
      items: [
        'api/types',
        'api/core-methods',
        'api/listeners',
        'api/error-codes',
      ],
    },
    {
      type: 'category',
      label: 'Examples',
      items: [
        'examples/basic-store',
        'examples/subscription-store',
        'examples/complete-implementation',
      ],
    },
  ],
};

export default sidebars;