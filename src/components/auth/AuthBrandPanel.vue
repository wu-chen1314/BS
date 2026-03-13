<template>
  <section class="brand-side">
    <div class="brand-orbit orbit-one"></div>
    <div class="brand-orbit orbit-two"></div>

    <div class="brand-content">
      <div class="copy-block">
        <span class="copy-badge">登录观察团</span>
        <h1>守艺搭子陪你登录</h1>
        <p>
          三位小搭子会盯着输入框一起帮你把关。开始输入时他们会跟着看，输错了就会先替你委屈一下。
        </p>
      </div>

      <div class="mascot-grid" :class="[`mood-${mood}`]">
        <article
          v-for="mascot in mascots"
          :key="mascot.id"
          class="mascot-card"
          :class="[`mood-${mood}`]"
          :style="buildMascotStyle(mascot)"
        >
          <div class="card-glow"></div>
          <div class="mascot-head">
            <span class="hair hair-left"></span>
            <span class="hair hair-right"></span>
            <div class="face">
              <span class="cheek cheek-left"></span>
              <span class="cheek cheek-right"></span>

              <div class="brow-row">
                <span class="brow brow-left"></span>
                <span class="brow brow-right"></span>
              </div>

              <div class="eye-row">
                <span class="eye eye-left">
                  <span class="pupil"></span>
                  <span class="eyelid"></span>
                </span>
                <span class="eye eye-right">
                  <span class="pupil"></span>
                  <span class="eyelid"></span>
                </span>
              </div>

              <span class="mouth"></span>
              <span class="tear tear-left"></span>
              <span class="tear tear-right"></span>
            </div>
          </div>

          <div class="mascot-body">
            <span class="body-ribbon">{{ mascot.role }}</span>
            <strong>{{ mascot.name }}</strong>
            <p>{{ mascot.copy }}</p>
          </div>
        </article>
      </div>

      <div class="stage-status">
        <span class="status-pill" :class="[`mood-${mood}`]">{{ stageHint }}</span>
        <p>{{ stageDetail }}</p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onBeforeUnmount } from "vue";

const mouseX = ref(typeof window !== "undefined" ? window.innerWidth / 2 : 0);
const mouseY = ref(typeof window !== "undefined" ? window.innerHeight / 2 : 0);

const handleMouseMove = (e: MouseEvent) => {
  mouseX.value = e.clientX;
  mouseY.value = e.clientY;
};

onMounted(() => {
  if (typeof window !== "undefined") {
    window.addEventListener("mousemove", handleMouseMove);
  }
});

onBeforeUnmount(() => {
  if (typeof window !== "undefined") {
    window.removeEventListener("mousemove", handleMouseMove);
  }
});

type FocusField =
  | "username"
  | "passwordHash"
  | "nickname"
  | "email"
  | "code"
  | "confirmPassword"
  | "captchaAnswer";

type BrandMood = "calm" | "watching" | "sad" | "cheer";

interface MascotDefinition {
  id: string;
  name: string;
  role: string;
  copy: string;
  accent: string;
  accentSoft: string;
  skin: string;
  hair: string;
  tiltX: number;
  tiltY: number;
}

const props = withDefaults(
  defineProps<{
    focusField?: FocusField | null;
    mood?: BrandMood;
  }>(),
  {
    focusField: null,
    mood: "calm",
  }
);

const mascots: MascotDefinition[] = [
  {
    id: "wood",
    name: "木木",
    role: "看账号",
    copy: "先帮你盯住账号框，别把昵称输成登录名。",
    accent: "#ff8b6a",
    accentSoft: "rgba(255, 139, 106, 0.2)",
    skin: "#ffe7d4",
    hair: "#8f4c38",
    tiltX: 0,
    tiltY: -2,
  },
  {
    id: "yun",
    name: "阿绣",
    role: "看密码",
    copy: "密码和验证码她都要多看一眼，主打一个仔细。",
    accent: "#4fb8a8",
    accentSoft: "rgba(79, 184, 168, 0.2)",
    skin: "#fff1de",
    hair: "#38566f",
    tiltX: 1,
    tiltY: 1,
  },
  {
    id: "feng",
    name: "团团",
    role: "看结果",
    copy: "如果你输对了他会很开心，输错了他也会跟着皱眉。",
    accent: "#f2b54a",
    accentSoft: "rgba(242, 181, 74, 0.2)",
    skin: "#ffe9cc",
    hair: "#77462a",
    tiltX: -1,
    tiltY: 2,
  },
];

const lookTargetMap: Record<FocusField, { x: number; y: number }> = {
  username: { x: 8, y: -7 },
  nickname: { x: 8, y: -4 },
  passwordHash: { x: 10, y: -1 },
  confirmPassword: { x: 10, y: 2 },
  email: { x: 10, y: 6 },
  code: { x: 11, y: 9 },
  captchaAnswer: { x: 12, y: 13 },
};

const focusLabelMap: Record<FocusField, string> = {
  username: "账号框",
  nickname: "昵称框",
  passwordHash: "密码框",
  confirmPassword: "确认密码框",
  email: "邮箱框",
  code: "验证码框",
  captchaAnswer: "口算验证码框",
};

const stageHint = computed(() => {
  switch (props.mood) {
    case "watching":
      return props.focusField ? `正在盯着${focusLabelMap[props.focusField]}` : "正在跟随输入";
    case "sad":
      return "刚刚好像输错了";
    case "cheer":
      return "状态很好，继续保持";
    default:
      return "准备就绪";
  }
});

const stageDetail = computed(() => {
  switch (props.mood) {
    case "watching":
      return "他们的眼神会一直追随你的鼠标轨迹，像真的在帮你盯着每一步。";
    case "sad":
      return "一旦校验失败或登录报错，三位会同步变成失落表情提醒你回头检查。";
    case "cheer":
      return "这一步看起来很顺利，搭子们已经开始替你开心了。";
    default:
      return "先选中输入框试试，他们会马上把目光投过去。";
  }
});

const buildMascotStyle = (mascot: MascotDefinition) => {
  let x = mascot.tiltX;
  let y = mascot.tiltY;

  if (props.mood === "sad") {
    x = Math.max(-4, mascot.tiltX - 2);
    y = mascot.tiltY + 2;
  } else if (props.mood === "cheer") {
    x = Math.round(mascot.tiltX * 0.35);
    y = Math.round(mascot.tiltY * 0.35) - 1;
  } else {
    if (typeof window !== "undefined") {
      const rectX = window.innerWidth / 4; 
      const rectY = window.innerHeight / 2;
      const dx = mouseX.value - rectX;
      const dy = mouseY.value - rectY;
      
      const angle = Math.atan2(dy, dx);
      const distance = Math.min(Math.sqrt(dx * dx + dy * dy) / 40, 9);
      
      x += Math.cos(angle) * distance;
      y += Math.sin(angle) * distance;
    }
  }

  return {
    "--card-accent": mascot.accent,
    "--card-accent-soft": mascot.accentSoft,
    "--face-skin": mascot.skin,
    "--hair-color": mascot.hair,
    "--pupil-x": `${Math.round(x)}px`,
    "--pupil-y": `${Math.round(y)}px`,
  };
};
</script>

<style scoped>
.brand-side {
  flex: 1.18;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 20%, rgba(255, 220, 182, 0.35), transparent 24%),
    radial-gradient(circle at 82% 18%, rgba(77, 179, 163, 0.16), transparent 18%),
    linear-gradient(145deg, #203a52 0%, #102234 40%, #162d46 100%);
  color: #fff5eb;
  padding: 44px 36px;
}

.brand-orbit {
  position: absolute;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  pointer-events: none;
}

.orbit-one {
  width: 240px;
  height: 240px;
  top: -90px;
  right: -40px;
  animation: orbitFloat 10s ease-in-out infinite;
}

.orbit-two {
  width: 180px;
  height: 180px;
  left: -80px;
  bottom: -40px;
  animation: orbitFloat 12s ease-in-out infinite reverse;
}

.brand-content {
  position: relative;
  z-index: 1;
  display: flex;
  min-height: 100%;
  flex-direction: column;
  justify-content: space-between;
  gap: 28px;
}

.copy-block {
  max-width: 420px;
}

.copy-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  border-radius: 999px;
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.1);
  color: #ffe1b8;
  font-size: 13px;
  letter-spacing: 0.2em;
}

.copy-block h1 {
  margin: 0;
  font-size: 48px;
  line-height: 1.08;
  letter-spacing: 0.04em;
}

.copy-block p {
  margin: 16px 0 0;
  max-width: 360px;
  color: rgba(255, 245, 235, 0.82);
  font-size: 16px;
  line-height: 1.8;
}

.mascot-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.mascot-card {
  position: relative;
  overflow: hidden;
  border-radius: 28px;
  padding: 16px 16px 18px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(16px);
  box-shadow: 0 24px 40px -28px rgba(0, 0, 0, 0.55);
  transition:
    transform 220ms ease,
    box-shadow 220ms ease,
    background 220ms ease;
}

.mascot-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 30px 48px -26px rgba(0, 0, 0, 0.58);
}

.card-glow {
  position: absolute;
  inset: auto auto 0 -10%;
  width: 120%;
  height: 52%;
  background: radial-gradient(circle, var(--card-accent-soft) 0%, transparent 70%);
  pointer-events: none;
}

.mascot-head {
  position: relative;
  margin: 0 auto 18px;
  width: 112px;
  height: 104px;
}

.face {
  position: absolute;
  inset: 16px 10px 0;
  border-radius: 46px 46px 38px 38px;
  background: var(--face-skin);
  box-shadow: inset 0 -10px 0 rgba(255, 255, 255, 0.14);
}

.hair {
  position: absolute;
  top: 0;
  width: 36px;
  height: 30px;
  border-radius: 24px 24px 6px 6px;
  background: var(--hair-color);
}

.hair-left {
  left: 12px;
  transform: rotate(-18deg);
}

.hair-right {
  right: 12px;
  transform: rotate(18deg);
}

.brow-row,
.eye-row {
  display: flex;
  justify-content: space-between;
  padding: 0 18px;
}

.brow-row {
  margin-top: 20px;
  margin-bottom: 10px;
}

.brow {
  width: 18px;
  height: 4px;
  border-radius: 999px;
  background: rgba(74, 46, 31, 0.7);
  transition: transform 200ms ease;
}

.eye {
  position: relative;
  overflow: hidden;
  width: 22px;
  height: 18px;
  border-radius: 999px;
  background: #fff;
  box-shadow: inset 0 -2px 0 rgba(29, 45, 67, 0.08);
}

.pupil {
  position: absolute;
  top: 6px;
  left: 7px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #213448;
  transform: translate(var(--pupil-x), var(--pupil-y));
  transition: transform 180ms ease;
}

.eyelid {
  position: absolute;
  inset: 0;
  transform-origin: top center;
  background: var(--face-skin);
  animation: blink 5.2s ease-in-out infinite;
}

.mouth {
  display: block;
  width: 26px;
  height: 12px;
  margin: 14px auto 0;
  border-bottom: 3px solid rgba(74, 46, 31, 0.72);
  border-radius: 0 0 18px 18px;
  transition:
    border-radius 180ms ease,
    transform 180ms ease,
    border-color 180ms ease;
}

.cheek {
  position: absolute;
  top: 56px;
  width: 14px;
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 145, 146, 0.28);
}

.cheek-left {
  left: 10px;
}

.cheek-right {
  right: 10px;
}

.tear {
  position: absolute;
  top: 48px;
  width: 8px;
  height: 14px;
  border-radius: 6px 6px 10px 10px;
  background: rgba(115, 200, 255, 0.9);
  opacity: 0;
  transform: translateY(-4px);
}

.tear-left {
  left: 16px;
}

.tear-right {
  right: 16px;
}

.mascot-body {
  position: relative;
  text-align: center;
}

.body-ribbon {
  display: inline-flex;
  margin-bottom: 10px;
  border-radius: 999px;
  padding: 5px 10px;
  background: var(--card-accent-soft);
  color: var(--card-accent);
  font-size: 12px;
  font-weight: 700;
}

.mascot-body strong {
  display: block;
  color: #fff7ec;
  font-size: 20px;
}

.mascot-body p {
  margin: 8px 0 0;
  color: rgba(255, 244, 232, 0.75);
  font-size: 13px;
  line-height: 1.65;
}

.stage-status {
  display: flex;
  flex-direction: column;
  gap: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  padding-top: 18px;
}

.status-pill {
  display: inline-flex;
  width: fit-content;
  border-radius: 999px;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 700;
  color: #0f2232;
  background: #ffe0af;
}

.status-pill.mood-watching {
  background: #c0f4ea;
}

.status-pill.mood-sad {
  background: #ffd5d3;
}

.status-pill.mood-cheer {
  background: #fff0a8;
}

.stage-status p {
  margin: 0;
  color: rgba(255, 245, 235, 0.76);
  line-height: 1.7;
  font-size: 14px;
}

.mood-watching .mascot-card {
  transform: translateY(-2px);
}

.mood-cheer .mascot-card {
  animation: cheerBounce 1.15s ease-in-out infinite;
}

.mood-sad .mascot-card {
  background: rgba(255, 255, 255, 0.15);
}

.mood-sad .brow-left {
  transform: rotate(18deg);
}

.mood-sad .brow-right {
  transform: rotate(-18deg);
}

.mood-sad .mouth {
  margin-top: 16px;
  border-top: 3px solid rgba(74, 46, 31, 0.72);
  border-bottom: none;
  border-radius: 18px 18px 0 0;
}

.mood-sad .tear {
  opacity: 0.85;
  animation: tearDrop 1.8s ease-in-out infinite;
}

.mood-cheer .mouth {
  width: 30px;
  height: 14px;
  border-bottom-color: var(--card-accent);
}

@keyframes blink {
  0%,
  42%,
  48%,
  100% {
    transform: scaleY(0);
  }

  44%,
  46% {
    transform: scaleY(1);
  }
}

@keyframes tearDrop {
  0%,
  100% {
    transform: translateY(-4px);
  }

  50% {
    transform: translateY(6px);
  }
}

@keyframes orbitFloat {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(10px);
  }
}

@keyframes cheerBounce {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-6px);
  }
}

@media (max-width: 992px) {
  .brand-side {
    padding: 32px 24px 20px;
  }

  .copy-block h1 {
    font-size: 34px;
  }

  .copy-block p {
    max-width: none;
  }

  .mascot-grid {
    grid-template-columns: repeat(3, minmax(88px, 1fr));
  }

  .mascot-card {
    padding: 14px 12px 16px;
  }

  .mascot-head {
    width: 88px;
    height: 84px;
  }

  .face {
    inset: 12px 8px 0;
  }

  .brow-row {
    margin-top: 16px;
    margin-bottom: 8px;
    padding: 0 14px;
  }

  .eye-row {
    padding: 0 14px;
  }

  .eye {
    width: 18px;
    height: 14px;
  }

  .pupil {
    top: 4px;
    left: 5px;
    width: 7px;
    height: 7px;
  }

  .mouth {
    width: 22px;
  }

  .mascot-body strong {
    font-size: 16px;
  }

  .mascot-body p {
    font-size: 12px;
  }
}
</style>
