module.exports = {
  plugins: [
    require("tailwindcss")("tailwind.config.js"),
    require("@fullhuman/postcss-purgecss")({
      content: ["src/bs/**/*.cljs", "resources/public/index.html"],
      defaultExtractor: content =>
        content.match(/((sm|md|lg|xl|hover):)?[\w-/]+/g),
    }),
    require("autoprefixer"),
    require("cssnano")(),
  ],
};
