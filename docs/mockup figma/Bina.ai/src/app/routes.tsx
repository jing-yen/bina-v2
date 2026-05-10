import { createBrowserRouter } from "react-router";
import Root from "./components/bina/Root";
import { RecipeHub } from "./components/bina/RecipeHub";
import { MyPocket } from "./components/bina/MyPocket";
import { OfflineSync } from "./components/bina/OfflineSync";
import { Studio } from "./components/bina/Studio";
import { Analytics } from "./components/bina/Analytics";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: Root,
    children: [
      { index: true, Component: RecipeHub },
      { path: "pocket", Component: MyPocket },
      { path: "sync", Component: OfflineSync },
      { path: "studio", Component: Studio },
      { path: "analytics", Component: Analytics },
    ],
  },
]);
