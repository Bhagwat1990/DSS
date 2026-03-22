"use client";

import { useRef, useMemo } from "react";
import { Canvas, useFrame } from "@react-three/fiber";
import { Float, Stars } from "@react-three/drei";
import * as THREE from "three";

function ParticleField() {
  const ref = useRef<THREE.Points>(null);
  const count = 500;

  const positions = useMemo(() => {
    const pos = new Float32Array(count * 3);
    for (let i = 0; i < count * 3; i++) {
      pos[i] = (Math.random() - 0.5) * 20;
    }
    return pos;
  }, []);

  const colors = useMemo(() => {
    const c = new Float32Array(count * 3);
    for (let i = 0; i < count; i++) {
      const t = Math.random();
      c[i * 3] = t * 0;       // R
      c[i * 3 + 1] = t * 0.83; // G
      c[i * 3 + 2] = t * 1;    // B (cyan tint)
    }
    return c;
  }, []);

  useFrame((state) => {
    if (!ref.current) return;
    ref.current.rotation.y = state.clock.elapsedTime * 0.02;
    ref.current.rotation.x = Math.sin(state.clock.elapsedTime * 0.01) * 0.1;
  });

  return (
    <points ref={ref}>
      <bufferGeometry>
        <bufferAttribute
          attach="attributes-position"
          args={[positions, 3]}
        />
        <bufferAttribute
          attach="attributes-color"
          args={[colors, 3]}
        />
      </bufferGeometry>
      <pointsMaterial
        size={0.03}
        vertexColors
        transparent
        opacity={0.8}
        sizeAttenuation
      />
    </points>
  );
}

function DataOrb({ position, color, scale = 1 }: { position: [number, number, number]; color: string; scale?: number }) {
  const ref = useRef<THREE.Mesh>(null);

  useFrame((state) => {
    if (!ref.current) return;
    ref.current.scale.setScalar(scale + Math.sin(state.clock.elapsedTime * 2 + position[0]) * 0.1);
  });

  return (
    <Float speed={2} rotationIntensity={0.5} floatIntensity={0.5}>
      <mesh ref={ref} position={position}>
        <sphereGeometry args={[0.15, 32, 32]} />
        <meshStandardMaterial
          color={color}
          emissive={color}
          emissiveIntensity={0.5}
          transparent
          opacity={0.8}
        />
      </mesh>
    </Float>
  );
}

function GridPlane() {
  return (
    <gridHelper
      args={[20, 40, "#0097b2", "#0a1628"]}
      position={[0, -2, 0]}
      rotation={[0, 0, 0]}
    />
  );
}

export function HeroScene() {
  return (
    <div className="absolute inset-0 -z-10 opacity-40">
      <Canvas camera={{ position: [0, 0, 5], fov: 60 }}>
        <ambientLight intensity={0.2} />
        <pointLight position={[5, 5, 5]} intensity={0.5} color="#00d4ff" />
        <Stars radius={100} depth={50} count={1000} factor={2} saturation={0} fade speed={0.5} />
        <ParticleField />
        <DataOrb position={[-2, 1, -1]} color="#00d4ff" />
        <DataOrb position={[2, -0.5, -2]} color="#00ff88" scale={0.8} />
        <DataOrb position={[0, 1.5, -3]} color="#a855f7" scale={1.2} />
        <GridPlane />
      </Canvas>
    </div>
  );
}

// Animated signal visualization
function SignalRing({ signal }: { signal: string }) {
  const ref = useRef<THREE.Mesh>(null);
  const color =
    signal === "STRONG_BUY" || signal === "BUY"
      ? "#00ff88"
      : signal === "HOLD"
      ? "#ffd700"
      : "#ff4466";

  useFrame((state) => {
    if (!ref.current) return;
    ref.current.rotation.z = state.clock.elapsedTime * 0.5;
    ref.current.rotation.x = Math.sin(state.clock.elapsedTime * 0.3) * 0.3;
  });

  return (
    <mesh ref={ref}>
      <torusGeometry args={[1, 0.02, 16, 100]} />
      <meshStandardMaterial color={color} emissive={color} emissiveIntensity={1} />
    </mesh>
  );
}

export function SignalScene({ signal }: { signal: string }) {
  return (
    <div className="w-full h-48">
      <Canvas camera={{ position: [0, 0, 3] }}>
        <ambientLight intensity={0.3} />
        <pointLight position={[2, 2, 2]} intensity={0.6} />
        <SignalRing signal={signal} />
        <Float speed={3} rotationIntensity={1}>
          <mesh>
            <icosahedronGeometry args={[0.4, 1]} />
            <meshStandardMaterial
              color={
                signal === "STRONG_BUY" || signal === "BUY"
                  ? "#00ff88"
                  : signal === "HOLD"
                  ? "#ffd700"
                  : "#ff4466"
              }
              wireframe
              emissive={
                signal === "STRONG_BUY" || signal === "BUY"
                  ? "#00ff88"
                  : signal === "HOLD"
                  ? "#ffd700"
                  : "#ff4466"
              }
              emissiveIntensity={0.3}
            />
          </mesh>
        </Float>
      </Canvas>
    </div>
  );
}

// Animated background for cards
function WaveLines() {
  const meshRef = useRef<THREE.Mesh>(null);
  const geoRef = useRef<THREE.BufferGeometry>(null);

  const positions = useMemo(() => {
    const count = 101;
    const pos = new Float32Array(count * 3);
    for (let i = 0; i < count; i++) {
      pos[i * 3] = (i - 50) * 0.1;
      pos[i * 3 + 1] = 0;
      pos[i * 3 + 2] = 0;
    }
    return pos;
  }, []);

  useFrame((state) => {
    if (!geoRef.current) return;
    const posAttr = geoRef.current.attributes.position as THREE.BufferAttribute;
    for (let i = 0; i < posAttr.count; i++) {
      const x = posAttr.getX(i);
      posAttr.setY(i, Math.sin(x * 2 + state.clock.elapsedTime) * 0.2);
    }
    posAttr.needsUpdate = true;
  });

  return (
    <points ref={meshRef}>
      <bufferGeometry ref={geoRef}>
        <bufferAttribute
          attach="attributes-position"
          args={[positions, 3]}
        />
      </bufferGeometry>
      <pointsMaterial color="#00d4ff" size={0.02} transparent opacity={0.6} />
    </points>
  );
}

export function WaveBackground() {
  return (
    <div className="absolute inset-0 -z-10 opacity-30">
      <Canvas camera={{ position: [0, 0, 3] }}>
        <WaveLines />
      </Canvas>
    </div>
  );
}
