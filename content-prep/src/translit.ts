const MAP: Record<string, string> = {
  а:"a",б:"b",в:"v",г:"g",д:"d",е:"e",ё:"yo",ж:"zh",з:"z",и:"i",й:"y",
  к:"k",л:"l",м:"m",н:"n",о:"o",п:"p",р:"r",с:"s",т:"t",у:"u",ф:"f",
  х:"kh",ц:"ts",ч:"ch",ш:"sh",щ:"shch",ъ:"",ы:"y",ь:"'",э:"e",ю:"yu",я:"ya",
};
export function translit(input: string): string {
  let out = "";
  for (const ch of input) {
    const lower = ch.toLowerCase();
    const mapped = MAP[lower];
    if (mapped === undefined) { out += ch; continue; }
    out += ch === lower ? mapped : (mapped.charAt(0).toUpperCase() + mapped.slice(1));
  }
  return out;
}
