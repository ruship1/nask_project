/* Copyright 2002-2019 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation.analytical;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap;

/** Simple Keplerian orbit propagator.
 * @see Orbit
 * @author Guylaine Prat
 */
public class KeplerianPropagator extends AbstractAnalyticalPropagator implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20151117L;

    /** Initial state. */
    private SpacecraftState initialState;

    /** All states. */
    private transient TimeSpanMap<SpacecraftState> states;

    /** Build a propagator from orbit only.
     * <p>The central attraction coefficient μ is set to the same value used
     * for the initial orbit definition. Mass and attitude provider are set to
     * unspecified non-null arbitrary values.</p>
     * @param initialOrbit initial orbit
     */
    public KeplerianPropagator(final Orbit initialOrbit) {
        this(initialOrbit, DEFAULT_LAW, initialOrbit.getMu(), DEFAULT_MASS);
    }

    /** Build a propagator from orbit and central attraction coefficient μ.
     * <p>Mass and attitude provider are set to unspecified non-null arbitrary values.</p>
     * @param initialOrbit initial orbit
     * @param mu central attraction coefficient (m³/s²)
     */
    public KeplerianPropagator(final Orbit initialOrbit, final double mu) {
        this(initialOrbit, DEFAULT_LAW, mu, DEFAULT_MASS);
    }

    /** Build a propagator from orbit and attitude provider.
     * <p>The central attraction coefficient μ is set to the same value
     * used for the initial orbit definition. Mass is set to an unspecified
     * non-null arbitrary value.</p>
     * @param initialOrbit initial orbit
     * @param attitudeProv  attitude provider
     */
    public KeplerianPropagator(final Orbit initialOrbit,
                               final AttitudeProvider attitudeProv) {
        this(initialOrbit, attitudeProv, initialOrbit.getMu(), DEFAULT_MASS);
    }

    /** Build a propagator from orbit, attitude provider and central attraction
     * coefficient μ.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mu central attraction coefficient (m³/s²)
     */
    public KeplerianPropagator(final Orbit initialOrbit,
                               final AttitudeProvider attitudeProv,
                               final double mu) {
        this(initialOrbit, attitudeProv, mu, DEFAULT_MASS);
    }

    /** Build propagator from orbit, attitude provider, central attraction
     * coefficient μ and mass.
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mu central attraction coefficient (m³/s²)
     * @param mass spacecraft mass (kg)
     */
    public KeplerianPropagator(final Orbit initialOrbit, final AttitudeProvider attitudeProv,
                               final double mu, final double mass) {

        super(attitudeProv);

        // ensure the orbit use the specified mu and has no non-Keplerian derivatives
        initialState = fixState(initialOrbit,
                                getAttitudeProvider().getAttitude(initialOrbit,
                                                                  initialOrbit.getDate(),
                                                                  initialOrbit.getFrame()),
                                mass, mu, Collections.emptyMap());
        states = new TimeSpanMap<SpacecraftState>(initialState);
        super.resetInitialState(initialState);

    }

    /** Fix state to use a specified mu and remove derivatives.
     * <p>
     * This ensures the propagation model (which is based on calling
     * {@link Orbit#shiftedBy(double)}) is Keplerian only and uses a specified mu.
     * </p>
     * @param orbit orbit to fix
     * @param attitude current attitude
     * @param mass current mass
     * @param mu gravity coefficient to use
     * @param additionalStates additional states
     * @return fixed orbit
     */
    private SpacecraftState fixState(final Orbit orbit, final Attitude attitude, final double mass,
                                     final double mu, final Map<String, double[]> additionalStates) {
        final OrbitType type = orbit.getType();
        final double[] stateVector = new double[6];
        type.mapOrbitToArray(orbit, PositionAngle.TRUE, stateVector, null);
        final Orbit fixedOrbit = type.mapArrayToOrbit(stateVector, null, PositionAngle.TRUE,
                                                      orbit.getDate(), mu, orbit.getFrame());
        SpacecraftState fixedState = new SpacecraftState(fixedOrbit, attitude, mass);
        for (final Map.Entry<String, double[]> entry : additionalStates.entrySet()) {
            fixedState = fixedState.addAdditionalState(entry.getKey(), entry.getValue());
        }
        return fixedState;
    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state) {

        // ensure the orbit use the specified mu and has no non-Keplerian derivatives
        final double mu = initialState == null ? state.getMu() : initialState.getMu();
        final SpacecraftState fixedState = fixState(state.getOrbit(),
                                                    state.getAttitude(),
                                                    state.getMass(),
                                                    mu,
                                                    state.getAdditionalStates());

        initialState = fixedState;
        states       = new TimeSpanMap<SpacecraftState>(initialState);
        super.resetInitialState(fixedState);

    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward) {
        if (forward) {
            states.addValidAfter(state, state.getDate());
        } else {
            states.addValidBefore(state, state.getDate());
        }
    }

    /** {@inheritDoc} */
    protected Orbit propagateOrbit(final AbsoluteDate date) {

        // propagate orbit
        Orbit orbit = states.get(date).getOrbit();
        do {
            // we use a loop here to compensate for very small date shifts error
            // that occur with long propagation time
            orbit = orbit.shiftedBy(date.durationFrom(orbit.getDate()));
        } while (!date.equals(orbit.getDate()));

        return orbit;

    }

    /** {@inheritDoc}*/
    protected double getMass(final AbsoluteDate date) {
        return states.get(date).getMass();
    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     * @exception NotSerializableException if an additional state provider is not serializable
     */
    private Object writeReplace() throws NotSerializableException {
        try {

            // managed states providers
            final List<AdditionalStateProvider> serializableProviders = new ArrayList<AdditionalStateProvider>();
            for (final AdditionalStateProvider provider : getAdditionalStateProviders()) {
                if (provider instanceof Serializable) {
                    serializableProviders.add(provider);
                } else {
                    throw new NotSerializableException(provider.getClass().getName());
                }
            }

            // states transitions
            final AbsoluteDate[]    transitionDates;
            final SpacecraftState[] allStates;
            final SortedSet<TimeSpanMap.Transition<SpacecraftState>> transitions = states.getTransitions();
            if (transitions.size() == 1  && transitions.first().getBefore() == transitions.first().getAfter()) {
                // the single entry is a dummy one, without a real transition
                // we ignore it completely
                transitionDates = null;
                allStates       = null;
            } else {
                transitionDates = new AbsoluteDate[transitions.size()];
                allStates       = new SpacecraftState[transitions.size() + 1];
                int i = 0;
                for (final TimeSpanMap.Transition<SpacecraftState> transition : transitions) {
                    if (i == 0) {
                        // state before the first transition
                        allStates[i] = transition.getBefore();
                    }
                    transitionDates[i] = transition.getDate();
                    allStates[++i]     = transition.getAfter();
                }
            }

            return new DataTransferObject(getInitialState().getOrbit(), getAttitudeProvider(),
                                          getInitialState().getMu(), getInitialState().getMass(),
                                          transitionDates, allStates,
                                          serializableProviders.toArray(new AdditionalStateProvider[serializableProviders.size()]));
        } catch (OrekitException orekitException) {
            // this should never happen
            throw new OrekitInternalError(null);
        }

    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20151202L;

        /** Initial orbit. */
        private final Orbit orbit;

        /** Attitude provider. */
        private final AttitudeProvider attitudeProvider;

        /** Central attraction coefficient (m³/s²). */
        private final double mu;

        /** Spacecraft mass (kg). */
        private final double mass;

        /** Transition dates (may be null). */
        private final AbsoluteDate[] transitionDates;

        /** States before and after transitions (may be null). */
        private final SpacecraftState[] allStates;

        /** Providers for additional states. */
        private final AdditionalStateProvider[] providers;

        /** Simple constructor.
         * @param orbit initial orbit
         * @param attitudeProvider attitude provider
         * @param mu central attraction coefficient (m³/s²)
         * @param mass initial spacecraft mass (kg)
         * @param transitionDates transition dates (may be null)
         * @param allStates states before and after transitions (may be null)
         * @param providers providers for additional states
         */
        DataTransferObject(final Orbit orbit,
                           final AttitudeProvider attitudeProvider,
                           final double mu, final double mass,
                           final AbsoluteDate[] transitionDates,
                           final SpacecraftState[] allStates,
                           final AdditionalStateProvider[] providers) {
            this.orbit            = orbit;
            this.attitudeProvider = attitudeProvider;
            this.mu               = mu;
            this.mass             = mass;
            this.transitionDates  = transitionDates;
            this.allStates        = allStates;
            this.providers        = providers;
        }

        /** Replace the deserialized data transfer object with a {@link KeplerianPropagator}.
         * @return replacement {@link KeplerianPropagator}
         */
        private Object readResolve() {
            try {

                final KeplerianPropagator propagator =
                                new KeplerianPropagator(orbit, attitudeProvider, mu, mass);
                for (final AdditionalStateProvider provider : providers) {
                    propagator.addAdditionalStateProvider(provider);
                }

                if (transitionDates != null) {
                    // override the state transitions
                    propagator.states = new TimeSpanMap<SpacecraftState>(allStates[0]);
                    for (int i = 0; i < transitionDates.length; ++i) {
                        propagator.states.addValidAfter(allStates[i + 1], transitionDates[i]);
                    }
                }

                return propagator;

            } catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }
        }

    }

}
