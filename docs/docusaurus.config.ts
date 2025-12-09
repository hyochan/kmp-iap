import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

const config: Config = {
  title: 'KMP IAP',
  tagline: 'A Kotlin Multiplatform library for in-app purchases on iOS and Android',
  favicon: 'img/favicon.ico',

  // Custom fields can be used for any configuration not recognized by Docusaurus
  customFields: {
    future: {
      v4: true, // Improve compatibility with the upcoming Docusaurus v4
    },
  },

  // Set the production url of your site here
  url: 'https://hyochan.github.io',
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: '/kmp-iap/',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'hyochan', // Usually your GitHub org/user name.
  projectName: 'kmp-iap', // Usually your repo name.

  onBrokenLinks: 'warn',
  onBrokenMarkdownLinks: 'warn',

  // Even if you don't use internationalization, you can use this field to set
  // useful metadata like html lang. For example, if your site is Chinese, you
  // may want to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
          editUrl:
            'https://github.com/hyochan/kmp-iap/tree/main/docs/',
          lastVersion: 'current',
          versions: {
            current: {
              label: 'v1.0.0 (Current)',
              path: '',
            },
            '1.0.0-rc': {
              label: 'v1.0.0-rc',
              path: '1.0.0-rc',
            },
            '1.0.0-beta': {
              label: 'v1.0.0-beta',
              path: '1.0.0-beta',
            },
          },
          // Import MDX components
          beforeDefaultRemarkPlugins: [],
          remarkPlugins: [],
        },
        blog: {
          showReadingTime: true,
          feedOptions: {
            type: ['rss', 'atom'],
          },
          editUrl:
            'https://github.com/hyochan/kmp-iap/tree/main/docs/',
        },
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themes: [
    [
      require.resolve("@easyops-cn/docusaurus-search-local"),
      {
        hashed: true,
        language: ["en"],
        indexDocs: true,
        indexBlog: true,
        indexPages: true,
        docsRouteBasePath: '/docs',
        blogRouteBasePath: '/blog',
      },
    ],
  ],

  themeConfig: {
    // Replace with your project's social card
    image: 'img/logo.png',
    navbar: {
      title: 'kmp-iap',
      logo: {
        alt: 'kmp-iap Logo',
        src: 'img/logo.png',
      },
      items: [
        {
          type: 'docsVersionDropdown',
          position: 'left',
          dropdownActiveClassDisabled: true,
        },
        {
          type: 'docSidebar',
          sidebarId: 'docsSidebar',
          position: 'left',
          label: 'Docs',
        },
        {to: '/blog', label: 'Blog', position: 'left'},
        {
          href: 'https://github.com/hyochan/kmp-iap',
          label: 'GitHub',
          position: 'right',
        },
        {
          href: 'https://klibs.io/project/hyochan/kmp-iap',
          label: 'Klibs',
          position: 'right',
        },
        {
          href: 'https://x.com/hyodotdev',
          label: 'X',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'light',
      links: [
        {
          title: 'Docs',
          items: [
            {
              label: 'Getting Started',
              to: '/docs/getting-started/installation',
            },
            {
              label: 'API Reference',
              to: '/docs/api/',
            },
          ],
        },
        {
          title: 'Community',
          items: [
            {
              label: 'Stack Overflow',
              href: 'https://stackoverflow.com/questions/tagged/kmp-iap',
            },
            {
              label: 'GitHub Issues',
              href: 'https://github.com/hyochan/kmp-iap/issues',
            },
            {
              label: 'Slack',
              href: 'https://hyo.dev/joinSlack',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'GitHub',
              href: 'https://github.com/hyochan/kmp-iap',
            },
            {
              label: 'Klibs',
              href: 'https://klibs.io/project/hyochan/kmp-iap',
            },
          ],
        },
      ],
      copyright: `Copyright © 2025 hyochan.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['kotlin', 'swift', 'java', 'groovy'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;