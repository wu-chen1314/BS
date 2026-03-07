import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { fileURLToPath, URL } from "node:url";
import AutoImport from "unplugin-auto-import/vite";
import Components from "unplugin-vue-components/vite";
import { ElementPlusResolver } from "unplugin-vue-components/resolvers";

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      imports: ["vue", "vue-router"],
      resolvers: [ElementPlusResolver()],
      dts: false,
    }),
    Components({
      resolvers: [
        ElementPlusResolver({
          importStyle: "css",
        }),
      ],
      dts: false,
    }),
  ],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  build: {
    chunkSizeWarningLimit: 1000,
    reportCompressedSize: true,
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes("node_modules")) {
            return undefined;
          }
          if (id.includes("echarts")) {
            return "echarts";
          }
          if (id.includes("@vueup/vue-quill") || id.includes("quill")) {
            return "quill";
          }
          if (id.includes("@element-plus/icons-vue")) {
            return "element-icons";
          }
          if (id.includes("element-plus")) {
            return "element-plus";
          }
          if (id.includes("marked")) {
            return "markdown";
          }
          if (id.includes("axios")) {
            return "network";
          }
          if (id.includes("vue")) {
            return "vue-vendor";
          }
          return "vendor";
        },
      },
    },
  },
  server: {
    port: 5173,
    open: true,
  },
});
