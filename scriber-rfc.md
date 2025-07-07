# Project Scriber ADR

Project Scriber is an attempt to deliver a truly declarative data interface for government webforms, powered by the Fact Graph.
It includes an XML data specification for flow building and two generators that output that specification in different formats:
a multi-page form application and an all-screens data view.

Project Scriber builds on the experience and learnings of the DF Flow, and is inspired by its JSX interface.


## The DF Flow

The DF Flow was built in React and specified with JSX.
`Flow.tsx` contains the following statement of intent (slightly excerpted):

> The flow, while currently existing as code, has an eventual goal of becoming configuration that tax experts will be able to modify.
> This leads to a few design choices:
>   1. Everything in this file should remain serializable.
>   2. While many of the below react components look like our fact components, they have separate definitions from the components that actually render.
>   3. We have to maintain a mapping between the components we declare here in our config and the components that eventually render.

The Flow made tremendous progress in this direction, but it did not succeed.
Non-engineering tax experts could not modify the flow, and the flow was not serializable (more on what means below).

### Successes

The DF Flow successfully models a linear question flow while also auto-generating "Data Views" which let users go back and edit the information they input.
A couple core successes of the flow that we would like to carry forward are:

* Attributes like the `path` attribute, which links an input to a specific fact, and the `condition` attribute, which only shows a screen if the fact graph matches a logical statement, seamlessly connect the flow and Fact Graph
* It was (usually) easy to add a new modal or paragraph to a given screen, because the flow didn't entirely abstract away the DOM tree
* All-screens unlocked *many* workflows for technical and non-technical stakeholders alike.

### Limitations

*JSX is declarative, but it is not data.*

The DF flow is written in JSX.
JSX is not a data format, it is an [XML-like ECMAScript extension](https://facebook.github.io/jsx/).
This imposes major limitations on how the flow works.

First, the flow can only read by JavaScript tools.
In fact, the flow can't really be "read" at all.
JSX is syntactic sugar for creating React elements (or Svelte, etc.).
Therefore any tool that wants to understand the flow has to run React---and an entire browser.

More importantly, JSX is essentially impossible to edit programatically.
The tooling does not exist to pass a program some JSX and then have it output new JSX for you.
This is why it wasn't possible to build an interface for non-engineering owners to edit the flow, or view the relationships between its nodes.

All-screens—a read-only one-page view of the entire DF application—was a dramatic productivity unlock from the declarative flow, but there's a reason the tooling never evolved beyond that.
JSX is syntax for building React elements, so all-screens works by introspecting a tree of React elements.
By the time the flow has been converted into React elements, there's not much you can do with them *besides* render them in unique configurations.
Most of the "declarative" information has been lost.

Fortunately, a JSX-like data format exists that resolves all these problems.
It can be read and written by lightweight tooling that exists for every programming language;
it can be introspected as a graph without loss of any information;
and it can trivially rendered in a browser.
That format is XML (or XHTML).

## The XML Flow
