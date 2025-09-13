


import { CountingNumber } from "@/components/animate-ui/text/counting-number";
import { TypingText } from "@/components/animate-ui/text/typing";
import { RollingText } from "@/components/animate-ui/text/rolling";

interface Stats8Props {
  heading?: string;
  description?: string;
  link?: {
    text: string;
    url: string;
  };
  stats?: Array<{
    id: string;
    value: string;
    label: string;
  }>;
}

const Stats = ({
  heading = "Platform performance insights",
  description = "Ensuring stability and scalability for all users",
  stats = [
    {
      id: "stat-1",
      value: "250%+",
      label: "average growth in user engagement",
    },
    {
      id: "stat-2",
      value: "$2.5m",
      label: "annual savings per enterprise partner",
    },
    {
      id: "stat-3",
      value: "200+",
      label: "integrations with top industry platforms",
    },
    {
      id: "stat-4",
      value: "99.9%",
      label: "customer satisfaction over the last year",
    },
  ],
}: Stats8Props) => {
  return (
<section className="py-30">
  <div className="container max-w-7xl mx-auto px-6">
    <div className="flex flex-col gap-4 text-center">
      <h2 className="text-2xl font-bold md:text-4xl">
        <TypingText text={heading} duration={40} loop={true} holdDelay={5000} inView={true} inViewOnce={true} cursor />
      </h2>
      <p>
        <TypingText text={description} duration={20} loop={true} holdDelay={4500} cursor />
      </p>
    </div>

    <div className="mt-14 grid gap-x-5 gap-y-8 md:grid-cols-2 lg:grid-cols-4 text-center">
      {stats.map((stat) => {
        const match = stat.value.match(/([\d,.]+)([a-zA-Z%+]+)?/);
        const num = match ? parseFloat(match[1].replace(/,/g, "")) : 0;
        const suffix = match && match[2] ? match[2] : "";
        const decimalPlaces = match && match[1].includes(".") ? match[1].split(".")[1].length : 0;
        return (
          <div key={stat.id} className="flex flex-col gap-5">
            <div className="text-6xl font-bold">
              <CountingNumber number={num} decimalPlaces={decimalPlaces} inView={true} inViewOnce={true} />{suffix}
            </div>
            <p><RollingText text={stat.label} inView={true} inViewOnce={true} /></p>
          </div>
        );
      })}
    </div>
  </div>
</section>

  );
};

export { Stats };
