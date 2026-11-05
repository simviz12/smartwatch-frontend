// Generates the Pulso Watch Face Format XML (450x450) following the "Esfera" design.
const fs = require('fs');
const out = process.argv[2];
const C = 225;
const ticks = [];
for (let i = 0; i < 48; i++) {
  const major = i % 12 === 0;
  const len = major ? 22 : 14;
  const line = (color, alpha) => `
        <PartDraw x="0" y="0" width="450" height="450" angle="${(i * 7.5).toFixed(1)}" pivotX="0.5" pivotY="0.5">
          <Variant mode="AMBIENT" target="alpha" value="${major ? 140 : 70}" />
          <Line startX="${C}" startY="8" endX="${C}" endY="${8 + len}">
            <Stroke color="${color}" thickness="6" cap="ROUND" />
          </Line>
        </PartDraw>`;
  ticks.push(`
      <Condition>
        <Expressions>
          <Expression name="past"><![CDATA[(([HOUR_0_23] * 60 + [MINUTE]) / 30) >= ${i}]]></Expression>
        </Expressions>
        <Compare expression="past">${line('#E6F4F6F8')}
        </Compare>
        <Default>${line('#ff2B3038')}
        </Default>
      </Condition>`);
}

const bubble = (slotId, cx, provider, ring) => {
  const r = 52;
  return `
    <ComplicationSlot slotId="${slotId}" x="${cx - r}" y="${290 - r}" width="${r * 2}" height="${r * 2}" supportedTypes="SHORT_TEXT" isCustomizable="TRUE">
      <DefaultProviderPolicy defaultSystemProvider="EMPTY" defaultSystemProviderType="SHORT_TEXT"
          primaryProvider="${provider}" primaryProviderType="SHORT_TEXT" />
      <BoundingOval x="0" y="0" width="${r * 2}" height="${r * 2}" />
      <Complication type="SHORT_TEXT">
        <PartDraw x="0" y="0" width="${r * 2}" height="${r * 2}">
          <Variant mode="AMBIENT" target="alpha" value="0" />
          <Ellipse x="2" y="2" width="${r * 2 - 4}" height="${r * 2 - 4}">
            <Fill color="#ff15171B" />
            <Stroke color="${ring}" thickness="3" />
          </Ellipse>
        </PartDraw>
        <PartText x="0" y="22" width="${r * 2}" height="40">
          <Text align="CENTER">
            <Font family="SYNC_TO_DEVICE" size="32" weight="BOLD" color="#ffF4F6F8">
              <Template>%s<Parameter expression="[COMPLICATION.TEXT]" /></Template>
            </Font>
          </Text>
        </PartText>
        <PartText x="0" y="60" width="${r * 2}" height="24">
          <Text align="CENTER">
            <Font family="SYNC_TO_DEVICE" size="16" weight="BOLD" color="#ff9198A4">
              <Template>%s<Parameter expression="[COMPLICATION.TITLE]" /></Template>
            </Font>
          </Text>
        </PartText>
      </Complication>
    </ComplicationSlot>`;
};

const xml = `<?xml version="1.0" encoding="utf-8"?>
<!-- Pulso watch face (Watch Face Format). Data comes from the Pulso app complications. -->
<WatchFace width="450" height="450">
  <Metadata key="CLOCK_TYPE" value="DIGITAL" />
  <Metadata key="PREVIEW_TIME" value="12:58:00" />
  <Scene backgroundColor="#ff000000">
    <!-- 24 h ring: 48 half-hour ticks, 00:00 at the top; elapsed part of the day in calm teal. -->
    <Group name="ring" x="0" y="0" width="450" height="450">${ticks.join('')}
    </Group>

    <PartText x="0" y="76" width="450" height="34">
      <Variant mode="AMBIENT" target="alpha" value="160" />
      <Text align="CENTER">
        <Font family="SYNC_TO_DEVICE" size="24" weight="BOLD" color="#ff8FEBD3">
          <Template>%s %s<Parameter expression="[DAY_OF_WEEK_S]" /><Parameter expression="[DAY]" /></Template>
        </Font>
      </Text>
    </PartText>

    <!-- Time follows the watch 12/24 h setting. -->
    <Condition>
      <Expressions>
        <Expression name="h24"><![CDATA[[IS_24_HOUR_MODE] == 1]]></Expression>
      </Expressions>
      <Compare expression="h24">
        <PartText x="0" y="108" width="450" height="128">
          <Text align="CENTER">
            <Font family="SYNC_TO_DEVICE" size="112" weight="BOLD" color="#ffF4F6F8">
              <Template>%s:%s<Parameter expression="[HOUR_0_23]" /><Parameter expression="[MINUTE_Z]" /></Template>
            </Font>
          </Text>
        </PartText>
      </Compare>
      <Default>
        <PartText x="0" y="108" width="450" height="128">
          <Text align="CENTER">
            <Font family="SYNC_TO_DEVICE" size="112" weight="BOLD" color="#ffF4F6F8">
              <Template>%s:%s<Parameter expression="[HOUR_1_12]" /><Parameter expression="[MINUTE_Z]" /></Template>
            </Font>
          </Text>
        </PartText>
      </Default>
    </Condition>
${bubble(0, 164, 'com.pulso/com.pulso.wear.complication.PulseComplicationService', '#59FF8A7D')}
${bubble(1, 286, 'com.pulso/com.pulso.wear.complication.HrvComplicationService', '#59B4BBFF')}

    <ComplicationSlot slotId="2" x="140" y="362" width="170" height="44" supportedTypes="SHORT_TEXT" isCustomizable="TRUE">
      <DefaultProviderPolicy defaultSystemProvider="EMPTY" defaultSystemProviderType="SHORT_TEXT"
          primaryProvider="com.pulso/com.pulso.wear.complication.StateComplicationService" primaryProviderType="SHORT_TEXT" />
      <BoundingBox x="0" y="0" width="170" height="44" />
      <Complication type="SHORT_TEXT">
        <PartDraw x="0" y="0" width="170" height="44">
          <Variant mode="AMBIENT" target="alpha" value="0" />
          <RoundRectangle x="0" y="0" width="170" height="44" cornerRadiusX="22" cornerRadiusY="22">
            <Fill color="#298FEBD3" />
          </RoundRectangle>
          <Ellipse x="22" y="17" width="10" height="10">
            <Fill color="#ff8FEBD3" />
          </Ellipse>
        </PartDraw>
        <PartText x="30" y="6" width="130" height="32">
          <Text align="CENTER">
            <Font family="SYNC_TO_DEVICE" size="22" weight="BOLD" color="#ff8FEBD3">
              <Template>%s<Parameter expression="[COMPLICATION.TEXT]" /></Template>
            </Font>
          </Text>
        </PartText>
      </Complication>
    </ComplicationSlot>
  </Scene>
</WatchFace>
`;
fs.writeFileSync(out, xml);
console.log('written', xml.length);
