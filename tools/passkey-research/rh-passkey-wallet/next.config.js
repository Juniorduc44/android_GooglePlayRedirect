/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  transpilePackages: ["@turnkey/react-wallet-kit", "@turnkey/core"],
};
module.exports = nextConfig;
