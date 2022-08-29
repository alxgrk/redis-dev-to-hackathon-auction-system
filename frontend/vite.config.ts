import {defineConfig} from 'vite'
import preact from '@preact/preset-vite'
import fs from "fs";
import path from "path";

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [preact()],
    resolve: {
        alias: {
            react: 'preact/compat',
            'react-dom': 'preact/compat'
        }
    }
})

// function reactVirtualized() {
//     return {
//         name: "my:react-virtualized",
//         configResolved() {
//             const file = path.join( __dirname, "node_modules", "react-virtualized", "dist", "es", "WindowScroller", "utils", "onScroll.js")
//             const code = fs.readFileSync(file, "utf-8");
//             const modified = code.replace("import { bpfrpt_proptype_WindowScroller } from \"../WindowScroller.js\";", "");
//             fs.writeFileSync(file, modified);
//         },
//     }
// }
