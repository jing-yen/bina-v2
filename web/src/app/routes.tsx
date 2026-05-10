import { createBrowserRouter } from "react-router";
import Root from "./components/bina/Root";
import { Dashboard } from "./components/bina/Dashboard";
import { Studio } from "./components/bina/Studio";
import { Analytics } from "./components/bina/Analytics";
import { Sync } from "./components/bina/Sync";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: Root,
    children: [
      { index: true, Component: Dashboard },
      { path: "studio", Component: Studio },
      { path: "analytics", Component: Analytics },
      { path: "sync", Component: Sync },
    ],
  },
]);
