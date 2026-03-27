import type { Config } from 'tailwindcss'
import forms from '@tailwindcss/forms'

const config: Config = {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // === 主色 ===
        'primary': '#0057c2',
        'primary-container': '#006ef2',
        'primary-fixed': '#d9e2ff',
        'primary-fixed-dim': '#afc6ff',
        'on-primary': '#ffffff',
        'on-primary-container': '#fefcff',
        'on-primary-fixed': '#001a43',
        'on-primary-fixed-variant': '#004398',
        'inverse-primary': '#afc6ff',

        // === 次色 ===
        'secondary': '#4d6077',
        'secondary-container': '#cde1fd',
        'secondary-fixed': '#d1e4ff',
        'secondary-fixed-dim': '#b4c8e3',
        'on-secondary': '#ffffff',
        'on-secondary-container': '#51647c',
        'on-secondary-fixed': '#071d31',
        'on-secondary-fixed-variant': '#35485e',

        // === 强调色 ===
        'tertiary': '#7431d3',
        'tertiary-container': '#8e4fee',
        'tertiary-fixed': '#ecdcff',
        'tertiary-fixed-dim': '#d5baff',
        'on-tertiary': '#ffffff',
        'on-tertiary-container': '#fffbff',
        'on-tertiary-fixed': '#270057',
        'on-tertiary-fixed-variant': '#5e08bd',

        // === 错误色 ===
        'error': '#ba1a1a',
        'error-container': '#ffdad6',
        'on-error': '#ffffff',
        'on-error-container': '#93000a',

        // === Surface 系列 ===
        'surface': '#f9f9f9',
        'surface-bright': '#f9f9f9',
        'surface-dim': '#dadada',
        'surface-variant': '#e2e2e2',
        'surface-tint': '#0059c7',
        'surface-container': '#eeeeee',
        'surface-container-low': '#f3f3f3',
        'surface-container-lowest': '#ffffff',
        'surface-container-high': '#e8e8e8',
        'surface-container-highest': '#e2e2e2',

        // === 文字与边框 ===
        'on-surface': '#1a1c1c',
        'on-surface-variant': '#414755',
        'on-background': '#1a1c1c',
        'outline': '#727786',
        'outline-variant': '#c1c6d7',
        'background': '#f9f9f9',
        'inverse-surface': '#2f3131',
        'inverse-on-surface': '#f1f1f1',
      },

      fontFamily: {
        'headline': ['Manrope', 'sans-serif'],
        'body': ['Inter', 'sans-serif'],
        'label': ['Inter', 'sans-serif'],
      },

      borderRadius: {
        DEFAULT: '0.25rem',  // 4px
        lg: '0.5rem',        // 8px
        xl: '0.75rem',       // 12px
        full: '9999px',
      },
    },
  },
  plugins: [
    forms,
  ],
}

export default config
