import { ReactElement, useEffect } from "react";
import { useHistory } from "@docusaurus/router";
import useBaseUrl from "@docusaurus/useBaseUrl";

export default function Home(): ReactElement | null {
  const history = useHistory();
  const docsUrl = useBaseUrl("/docs/intro");

  useEffect(() => {
    history.replace(docsUrl);
  }, [history, docsUrl]);

  return null;
}
