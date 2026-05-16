export interface DemoDocument {
  name: string;
  category: string;
  emoji: string;
  author: string;
  org: string;
  links: { label: string; url: string }[];
  content: string;
}

export const DEMO_DOCUMENTS: DemoDocument[] = [
  {
    name: 'Panduan Penyakit Tanaman',
    category: 'Agriculture',
    emoji: '\u{1F33F}',
    author: 'Jabatan Pertanian Malaysia (Dept. of Agriculture)',
    org: 'Jabatan Pertanian Malaysia (Dept. of Agriculture)',
    links: [
      { label: 'Portal Jabatan Pertanian', url: 'https://www.doa.gov.my' },
      { label: 'Panduan Tanaman MARDI', url: 'https://www.mardi.gov.my' },
    ],
    content: `Common Crop Diseases in Southeast Asia

Rice Blast (Magnaporthe oryzae)
Symptoms: Diamond-shaped lesions on leaves, gray-green to white centers with dark borders. Panicle neck rot causes empty grains.
Treatment: Apply tricyclazole fungicide at first sign. Use resistant varieties (IR64, NSIC Rc222). Avoid excessive nitrogen fertiliser.
Prevention: Proper spacing, balanced fertilisation, flood management.

Bacterial Leaf Blight (Xanthomonas oryzae)
Symptoms: Water-soaked lesions on leaf edges turning yellow-white. Leaves dry up from tips.
Treatment: No chemical cure once established. Drain fields, reduce nitrogen. Apply copper-based bactericide preventively.
Prevention: Use certified disease-free seeds, resistant varieties.

Leaf Rust (Puccinia spp.) - Wheat/Corn
Symptoms: Orange-brown pustules on leaves and stems. Severe infection causes premature leaf death.
Treatment: Apply propiconazole or tebuconazole fungicide. Remove volunteer plants nearby.

Fruit Fly (Bactrocera dorsalis) - Mango/Papaya
Symptoms: Small puncture marks on fruit, larvae inside, premature fruit drop.
Treatment: Protein bait traps, methyl eugenol traps. Bag fruits individually. Hot water treatment post-harvest (46°C for 60 minutes).

Coconut Rhinoceros Beetle
Symptoms: V-shaped cuts on fronds, bore holes in crown. Tree weakening and reduced yield.
Treatment: Remove breeding sites (decaying logs, manure heaps). Apply Oryctes virus or Metarhizium fungus. Hook out larvae from bore holes.`,
  },
  {
    name: 'First Aid & Triage Manual',
    category: 'Health',
    emoji: '\u{1F3E5}',
    author: 'Dr. Maria Santos',
    org: 'Community Health Alliance',
    links: [
      { label: 'WHO First Aid Guidelines', url: 'https://www.who.int/first-aid' },
      { label: 'Red Cross Training', url: 'https://www.redcross.org/take-a-class' },
    ],
    content: `Community Health Worker Field Guide

Emergency Triage (START Method)
RED (Immediate): Not breathing after airway cleared, rapid breathing (>30/min), no radial pulse, cannot follow commands.
YELLOW (Delayed): Breathing normally, has pulse, can follow commands but cannot walk.
GREEN (Minor): Can walk, minor injuries only.
BLACK (Deceased): Not breathing even after airway repositioned.

Common Conditions & First Response

Dehydration (especially children under 5)
Signs: Sunken eyes, dry mouth, skin pinch stays tented, reduced urination.
Action: ORS solution — 1 litre clean water + 6 teaspoons sugar + ½ teaspoon salt. Give small sips every 5 minutes. If child cannot drink, refer immediately.
Danger signs: Unconscious, persistent vomiting, blood in stool → REFER NOW.

Snake Bite
DO: Keep patient calm and still. Immobilise bitten limb below heart level. Remove rings/bracelets. Note time and snake description.
DO NOT: Cut wound, suck venom, apply tourniquet, apply ice.
Action: Transport to facility with antivenom. Pressure immobilisation bandage if trained.

Burns
Minor (small area, no blisters): Cool under clean running water 20 minutes. Cover with clean cloth.
Moderate (blisters, larger area): Cool water, do NOT pop blisters. Cover loosely. Give pain relief.
Severe (charred, large area, face/hands/joints): Cover with clean sheet. Give sips of water. REFER IMMEDIATELY.

Fever in Children
Mild (37.5-38.5°C): Tepid sponge bath, light clothing, fluids. Paracetamol per weight.
High (>38.5°C or >3 days): Check for malaria (rapid test), dengue signs (rash, bleeding). REFER if persistent.
Danger signs: Stiff neck, bulging fontanelle, seizures, difficulty breathing → REFER NOW.`,
  },
  {
    name: 'Market Pricing & Trade Guide',
    category: 'Finance',
    emoji: '\u{1F4B0}',
    author: 'Raj Patel',
    org: 'Smallholder Trade Cooperative',
    links: [
      { label: 'FAO Market Information', url: 'https://www.fao.org/markets' },
      { label: 'Trade Standards Guide', url: 'https://www.intracen.org/resources' },
    ],
    content: `Smallholder Market Guide — Southeast Asia

Seasonal Price Patterns
Rice: Lowest at harvest (Oct-Dec), highest before planting (Jun-Aug). Store 2-3 months for 15-25% price increase.
Vegetables: Prices spike during monsoon (reduced supply). Off-season greenhouse production commands 2-3x premium.
Palm Oil: Global commodity, follows CPO futures. Local price lags international by 2-4 weeks.

Grading Standards (Fresh Produce)
Grade A: No blemishes, uniform size, optimal ripeness. Commands 30-50% premium.
Grade B: Minor cosmetic defects, slight size variation. Standard market price.
Grade C: Significant defects, overripe/underripe. Suitable for processing only. 40-60% of Grade A price.

Profit Margin Calculator
Basic formula: Profit = (Selling Price − Cost Price) × Quantity × (1 − Tax/Fee Rate)
Target margins: Fresh produce 20-40%, processed goods 30-60%, value-added products 50-100%.
Break-even: Fixed costs ÷ (Price per unit − Variable cost per unit)

Finding Buyers
1. Local wet markets: Direct selling, cash payment, no middleman. Limited volume.
2. Collector/middleman: Picks up at farm gate. Lower price but convenient. Negotiate volume discounts.
3. Supermarket contracts: Requires consistent quality, volume, GAP certification. 60-90 day payment terms.
4. Online platforms: Shopee Farm, Lazada Fresh. Requires packaging, cold chain. Higher margin.
5. Export: Requires phytosanitary certificate, HACCP, minimum volume. Best margins but highest barrier.

Negotiation Tips
- Know current market price before negotiating (check commodity boards).
- Sell collectively through cooperatives for better bargaining power.
- Grade and sort before selling — mixed grades lower overall price.
- Time your sales: avoid peak harvest when everyone is selling.`,
  },
  {
    name: 'Building Safety Guide',
    category: 'Education',
    emoji: '\u{1F3D7}\u{FE0F}',
    author: 'Eng. Kamal Hassan',
    org: 'UN-Habitat Safer Buildings Programme',
    links: [
      { label: 'FEMA Building Safety', url: 'https://www.fema.gov/building-science' },
      { label: 'World Bank GFDRR', url: 'https://www.gfdrr.org/en/safer-homes' },
    ],
    content: `Structural Safety Assessment Field Guide

Pre-Inspection Checklist
Before entering any building after a disaster or structural concern:
1. Check for gas leaks (smell, hissing sounds)
2. Look for visible structural damage from outside
3. Check if the building is leaning or shifted from foundation
4. Note any fallen power lines nearby
5. Wear protective equipment: hard hat, sturdy shoes, dust mask

Damage Classification

GREEN — Safe for Occupancy
No visible cracks in load-bearing walls. Roof intact. Foundation level. Doors and windows open/close normally. No water damage to structural elements.
Action: Document condition, issue green tag, allow occupancy.

YELLOW — Restricted Use
Hairline cracks in walls (less than 3mm width). Minor roof damage. Some doors/windows stuck. Cosmetic damage to non-structural elements. Partial water damage.
Action: Limit occupancy to daytime only. No heavy storage on upper floors. Schedule professional assessment within 30 days.

RED — Unsafe / Do Not Enter
Cracks wider than 5mm in load-bearing walls. Visible foundation shifting. Roof partially collapsed. Floors sagging or bouncing. Walls leaning more than 1 degree from vertical.
Action: Evacuate immediately. Barricade entry points. Report to local authority. Do not attempt repairs without professional engineer.

Common Building Materials — Southeast Asia

Reinforced Concrete (RC)
Most common for multi-storey. Check for exposed rebar (rust = weakness). Spalling concrete indicates water ingress. Column damage is more critical than beam damage.

Timber Frame
Common in rural areas. Check for termite damage, rot at joints. Roof-to-wall connections are critical failure points. Raised floors reduce flood damage.

Masonry (Brick/Block)
Unreinforced masonry is highest risk in earthquakes. Look for stair-step cracks following mortar lines. Ring beams at roof level significantly improve safety.

Bamboo
Traditional and sustainable. Joints are the weakest point — check lashing/bolts. Treat against borers. Lifespan 3-5 years untreated, 15-25 years treated.

Emergency Shoring
If a wall is leaning but building must remain temporarily occupied:
- Use 100x100mm timber props at 45-degree angle
- Base on solid footing (concrete pad or timber spreader)
- Wedge tightly at top and bottom
- Install in pairs on both sides if possible
- This is TEMPORARY — evacuate as soon as alternative shelter is available`,
  },
];
