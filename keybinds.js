const fs = require('node:fs/promises');
const path = require('node:path');

const PROFILE_VERSION = 1;
const STATE_FILENAME = '.launcher-keybinds-v1.json';
const STANDARD_BINDINGS = {
  'key_key.forward': 'key.keyboard.w', 'key_key.left': 'key.keyboard.a',
  'key_key.back': 'key.keyboard.s', 'key_key.right': 'key.keyboard.d',
  'key_key.jump': 'key.keyboard.space', 'key_key.sneak': 'key.keyboard.left.shift',
  'key_key.sprint': 'key.keyboard.left.control', 'key_key.attack': 'key.mouse.left',
  'key_key.use': 'key.mouse.right', 'key_key.drop': 'key.keyboard.q',
  'key_key.inventory': 'key.keyboard.e', 'key_key.swapOffhand': 'key.keyboard.f',
  'key_key.chat': 'key.keyboard.t', 'key_key.command': 'key.keyboard.slash',
  'key_key.playerlist': 'key.keyboard.tab', 'key_key.pickItem': 'key.mouse.middle',
  'key_key.screenshot': 'key.keyboard.f2', 'key_key.togglePerspective': 'key.keyboard.f5',
  'key_key.fullscreen': 'key.keyboard.f11', 'key_key.advancements': 'key.keyboard.l',
  'key_key.socialInteractions': 'key.keyboard.p',
  'key_key.hotbar.1': 'key.keyboard.1', 'key_key.hotbar.2': 'key.keyboard.2',
  'key_key.hotbar.3': 'key.keyboard.3', 'key_key.hotbar.4': 'key.keyboard.4',
  'key_key.hotbar.5': 'key.keyboard.5', 'key_key.hotbar.6': 'key.keyboard.6',
  'key_key.hotbar.7': 'key.keyboard.7', 'key_key.hotbar.8': 'key.keyboard.8',
  'key_key.hotbar.9': 'key.keyboard.9',
  'key_accessories.key.open_accessories_screen': 'key.keyboard.h',
  'key_key.cobblemon.throwpartypokemon': 'key.keyboard.r',
  'key_key.cobblemon.summary': 'key.keyboard.i',
  'key_key.cobblemon.upshiftparty': 'key.keyboard.up',
  'key_key.cobblemon.downshiftparty': 'key.keyboard.down',
  'key_key.cobblemon.hideparty': 'key.keyboard.o',
  'key_key.cobblemon.ridingfreelook': 'key.keyboard.left.alt',
  'key_key.cobblemon_ranked.open_gui': 'key.keyboard.j',
  'key_key.cobblemon_smartphone.open': 'key.keyboard.k',
  'key_key.cobblemonraiddens.accept': 'key.keyboard.y',
  'key_key.cobblemonraiddens.deny': 'key.keyboard.n',
  'key_key.fightorflight.startbattle': 'key.keyboard.g',
  'key_key.travelersbackpack.inventory': 'key.keyboard.b',
  'key_gui.xaero_open_map': 'key.keyboard.m',
  'key_gui.xaero_waypoints_key': 'key.keyboard.u',
  'key_gui.xaero_minimap_settings': 'key.keyboard.right.bracket',
  'key_zoomify.key.zoom': 'key.keyboard.c',
  'key_supplementaries.keybind.quiver': 'key.keyboard.v',
  'key_key.ftbquests.quests': 'key.keyboard.grave.accent',
  'key_key.more_cobblemon_tweaks.open_config': 'key.keyboard.f9',
  'key_key.presencefootsteps.settings': 'key.keyboard.f10',
  'key_key.yamlconfig.configs': 'key.keyboard.f12',
  'key_key.jade.config': 'key.keyboard.keypad.0',
  'key_key.jade.show_overlay': 'key.keyboard.keypad.1',
  'key_key.jade.toggle_liquid': 'key.keyboard.keypad.2',
  'key_key.jade.show_recipes': 'key.keyboard.keypad.3',
  'key_key.jade.show_uses': 'key.keyboard.keypad.4',
  'key_key.jade.narrate': 'key.keyboard.keypad.5'
};

const DISABLED_CONFLICTS = [
  'key_key.cobblemon-battle-extras.battle_logs', 'key_key.cobblemon_smartphone.scanner',
  'key_key.catchrate.show_comparison', 'key_key.catchrate.toggle_hud',
  'key_key.cobblemonraiddens.mouse', 'key_key.cobblemonrizetweaks.jumpPCBox',
  'key_key.techreborn.quantumSuitSprint', 'key_key.techreborn.suitNightVision',
  'key_gui.xaero_new_waypoint', 'key_gui.xaero_enlarge_map',
  'key_key.travelersbackpack.cycle_tool', 'key_key.jei.bookmark',
  'key_key.jei.showRecipe', 'key_key.jei.showUses', 'key_key.jei.showRecipe2',
  'key_key.jei.showUses2', 'key_key.jei.cheatOneItem', 'key_key.jei.cheatOneItem2',
  'key_key.jei.cheatItemStack2', 'key_key.jei.clearSearchBar',
  'key_key.jei.nextSearch', 'key_key.jei.previousSearch',
  'key_key.jei.pauseRecipeCycling', 'key_key.jei.showBookmarkTooltipFeatures',
  'key_key.ftbquests.gui.move_up', 'key_key.ftbquests.gui.move_down',
  'key_key.ftbquests.gui.next_chapter', 'key_key.ftbquests.gui.player_prefs',
  'key_key.ftbquests.gui.recenter', 'key_key.ftbquests.gui.search',
  'key_key.ftbquests.gui_editor.copy', 'key_key.ftbquests.gui_editor.paste',
  'key_key.ftbquests.gui_editor.select_all', 'key_key.ftbquests.gui_editor.select_none',
  'key_key.ftbquests.gui_editor.reload_theme', 'key_key.ftbquests.gui_editor.reward_tables',
  'key_key.ftbquests.gui_editor.toggle_crosshairs', 'key_key.ftbquests.gui_editor.redo',
  'key_key.ftbquests.gui_editor.undo', 'key_key.ftbquests.gui_quest_panel.add_line',
  'key_key.ftbquests.gui_quest_panel.add_page_break', 'key_key.ftbquests.gui_quest_panel.edit_desc',
  'key_key.ftbquests.gui_quest_panel.edit_quest_props', 'key_key.ftbquests.gui_quest_panel.edit_subtitle',
  'key_key.ftbquests.gui_quest_panel.edit_title', 'key_key.refinedstorage.focus_search_bar',
  'key_key.loadToolbarActivator', 'key_key.saveToolbarActivator'
];

async function readState(filename) {
  try { return JSON.parse(await fs.readFile(filename, 'utf8')); }
  catch (error) { if (error.code === 'ENOENT') return null; throw error; }
}

async function applyDefaultKeybinds(gamePath) {
  const optionsPath = path.join(gamePath, 'options.txt');
  const statePath = path.join(gamePath, STATE_FILENAME);
  const state = await readState(statePath);
  try {
    await fs.access(optionsPath);
    if (state?.profileVersion === PROFILE_VERSION) return { changed: false, count: 0 };
  } catch (error) { if (error.code !== 'ENOENT') throw error; }

  let lines;
  try { lines = (await fs.readFile(optionsPath, 'utf8')).split(/\r?\n/).filter(Boolean); }
  catch (error) { if (error.code !== 'ENOENT') throw error; lines = ['version:3955']; }

  const desired = new Map(Object.entries(STANDARD_BINDINGS));
  for (const key of DISABLED_CONFLICTS) desired.set(key, 'key.keyboard.unknown');
  const applied = new Set();
  const output = lines.map((line) => {
    const separator = line.indexOf(':');
    if (separator < 0) return line;
    const key = line.slice(0, separator);
    if (!desired.has(key)) return line;
    applied.add(key);
    return `${key}:${desired.get(key)}`;
  });
  for (const [key, value] of desired) if (!applied.has(key)) output.push(`${key}:${value}`);

  await fs.mkdir(gamePath, { recursive: true });
  const temporaryOptions = `${optionsPath}.launcher-tmp`;
  await fs.writeFile(temporaryOptions, `${output.join('\n')}\n`, 'utf8');
  await fs.rename(temporaryOptions, optionsPath);
  const temporaryState = `${statePath}.tmp`;
  await fs.writeFile(temporaryState, `${JSON.stringify({ profileVersion: PROFILE_VERSION }, null, 2)}\n`, 'utf8');
  await fs.rename(temporaryState, statePath);
  return { changed: true, count: desired.size };
}

module.exports = { applyDefaultKeybinds, DISABLED_CONFLICTS, STANDARD_BINDINGS };
