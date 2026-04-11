import { PagePanel, PageTitleBar } from "../../components/AdminPrimitives";

export function PlaceholderPage({ title }) {
  return (
    <div className="space-y-6">
      <PagePanel>
        <PageTitleBar
          title={title}
          subtitle="Sección temporal disponible solo para navegación mientras se implementa su contenido."
        />
      </PagePanel>
    </div>
  );
}
