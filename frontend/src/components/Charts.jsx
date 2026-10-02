import { Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { money } from '../utils/format.js';

export const CHART_COLORS = ['#2E6B57', '#3B6EA5', '#C08A2B', '#8A4F7D', '#B3261E', '#5E7C8A'];

export function ChartCard({ title, children, empty }) {
  return (
    <div className="panel h-100">
      <h2 className="panel-title">{title}</h2>
      {empty ? <div className="text-body-secondary small py-5 text-center">No data for this period yet.</div> : (
        <div style={{ width: '100%', height: 260 }}>
          <ResponsiveContainer>{children}</ResponsiveContainer>
        </div>
      )}
    </div>
  );
}

export function IssuedReturnedChart({ data }) {
  return (
    <ChartCard title="Books issued and returned per month" empty={!data?.length}>
      <BarChart data={data} margin={{ top: 8, right: 8, left: -18, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#DCE3E0" />
        <XAxis dataKey="month" tick={{ fontSize: 12 }} interval={0} />
        <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
        <Tooltip />
        <Legend wrapperStyle={{ fontSize: 12 }} />
        <Bar dataKey="issued" name="Issued" fill={CHART_COLORS[0]} radius={[3, 3, 0, 0]} />
        <Bar dataKey="returned" name="Returned" fill={CHART_COLORS[1]} radius={[3, 3, 0, 0]} />
      </BarChart>
    </ChartCard>
  );
}

export function FineCollectionChart({ data }) {
  const rows = (data || []).map((d) => ({ ...d, finesCollected: Number(d.finesCollected) }));
  return (
    <ChartCard title="Fine collection per month" empty={!rows.length}>
      <BarChart data={rows} margin={{ top: 8, right: 12, left: -6, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#DCE3E0" />
        <XAxis dataKey="month" tick={{ fontSize: 12 }} interval={0} />
        <YAxis tick={{ fontSize: 12 }} tickFormatter={(v) => `₹${v}`} />
        <Tooltip formatter={(v) => money(v)} />
        <Bar dataKey="finesCollected" name="Collected" fill={CHART_COLORS[2]} radius={[3, 3, 0, 0]} />
      </BarChart>
    </ChartCard>
  );
}

export function RankingChart({ title, data, valueLabel }) {
  const rows = (data || []).map((d) => ({ name: d.name.length > 26 ? `${d.name.slice(0, 25)}…` : d.name, value: Number(d.value) }));
  return (
    <ChartCard title={title} empty={!rows.length}>
      <BarChart data={rows} layout="vertical" margin={{ top: 4, right: 16, left: 8, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#DCE3E0" />
        <XAxis type="number" allowDecimals={false} tick={{ fontSize: 12 }} />
        <YAxis type="category" dataKey="name" width={150} tick={{ fontSize: 12 }} />
        <Tooltip />
        <Bar dataKey="value" name={valueLabel} fill={CHART_COLORS[0]} radius={[0, 3, 3, 0]} />
      </BarChart>
    </ChartCard>
  );
}

export function CategoryPie({ data }) {
  const rows = (data || []).map((d) => ({ name: d.name, value: Number(d.value) }));
  return (
    <ChartCard title="Books by category" empty={!rows.length}>
      <PieChart>
        <Pie data={rows} dataKey="value" nameKey="name" innerRadius={55} outerRadius={90} paddingAngle={2}>
          {rows.map((r, i) => <Cell key={r.name} fill={CHART_COLORS[i % CHART_COLORS.length]} />)}
        </Pie>
        <Tooltip />
        <Legend wrapperStyle={{ fontSize: 12 }} />
      </PieChart>
    </ChartCard>
  );
}
